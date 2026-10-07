package hicc_project.RottenToday.service.recipe;

import hicc_project.RottenToday.dto.RecipeDto;
import hicc_project.RottenToday.entity.Ingredient;
import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.RecipeIngredient;
import hicc_project.RottenToday.repository.IngredientRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CSV에서 읽은 레시피를 RCP_SEQ 기준으로 upsert한다 (레시피 계획 Phase 4, D-020).
 * 같은 데이터로 여러 번 실행해도 결과가 같다. 묶음(chunk)마다 트랜잭션을 나눠, 실패한 묶음만 롤백하고 나머지는 저장한다.
 */
@Slf4j
@Service
public class RecipeImportService {

    static final int DEFAULT_CHUNK_SIZE = 100;
    private static final int RAW_TEXT_MAX = 500;
    private static final int TOP_UNLINKED = 20;

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeIngredientParser ingredientParser;
    private final TransactionTemplate transactionTemplate;
    private final int chunkSize;

    @Autowired
    public RecipeImportService(RecipeRepository recipeRepository, IngredientRepository ingredientRepository,
                               RecipeIngredientParser ingredientParser, PlatformTransactionManager transactionManager) {
        this(recipeRepository, ingredientRepository, ingredientParser, new TransactionTemplate(transactionManager), DEFAULT_CHUNK_SIZE);
    }

    RecipeImportService(RecipeRepository recipeRepository, IngredientRepository ingredientRepository,
                        RecipeIngredientParser ingredientParser, TransactionTemplate transactionTemplate, int chunkSize) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.ingredientParser = ingredientParser;
        this.transactionTemplate = transactionTemplate;
        this.chunkSize = chunkSize;
    }

    /**
     * @param failed      저장하지 못한 RCP_SEQ (그 묶음 전체가 롤백된다. 다시 실행하면 복구된다)
     * @param topUnlinked 재료 마스터에 연결되지 않은 이름과 등장 횟수, 많은 순 20개 (사전·마스터 보강용)
     */
    public record Summary(int total, int created, int updated, List<String> failed,
                          int ingredientCount, int linkedCount, List<Map.Entry<String, Integer>> topUnlinked) {
        public double linkedRate() {
            return ingredientCount == 0 ? 0 : (double) linkedCount / ingredientCount;
        }
    }

    public Summary importRecipes(List<RecipeDto> recipes) {
        // 마스터 이름 → 재료. 한 번만 읽는다
        Map<String, Ingredient> master = ingredientRepository.findAll().stream()
                .filter(i -> i.getName() != null)
                .collect(Collectors.toMap(Ingredient::getName, Function.identity(), (a, b) -> a));

        int created = 0, updated = 0, ingredientCount = 0, linkedCount = 0;
        List<String> failed = new ArrayList<>();
        Map<String, Integer> unlinked = new HashMap<>();

        for (int from = 0; from < recipes.size(); from += chunkSize) {
            List<RecipeDto> chunk = recipes.subList(from, Math.min(from + chunkSize, recipes.size()));
            try {
                ChunkResult result = transactionTemplate.execute(status -> importChunk(chunk, master));
                created += result.created;
                updated += result.updated;
                ingredientCount += result.ingredientCount;
                linkedCount += result.linkedCount;
                result.unlinked.forEach((name, count) -> unlinked.merge(name, count, Integer::sum));
            } catch (RuntimeException e) {
                List<String> seqs = chunk.stream().map(RecipeDto::getRCP_SEQ).toList();
                failed.addAll(seqs);
                log.warn("레시피 적재 묶음 실패 {}건 (RCP_SEQ {} ~ {}): {}", seqs.size(), seqs.get(0), seqs.get(seqs.size() - 1),
                        e.getClass().getSimpleName());
            }
        }

        List<Map.Entry<String, Integer>> top = unlinked.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_UNLINKED)
                .toList();
        return new Summary(recipes.size(), created, updated, failed, ingredientCount, linkedCount, top);
    }

    private static class ChunkResult {
        int created, updated, ingredientCount, linkedCount;
        final Map<String, Integer> unlinked = new HashMap<>();
    }

    private ChunkResult importChunk(List<RecipeDto> chunk, Map<String, Ingredient> master) {
        ChunkResult result = new ChunkResult();
        Map<String, Recipe> existing = recipeRepository.findAllByRcpSeqIn(chunk.stream().map(RecipeDto::getRCP_SEQ).toList())
                .stream().collect(Collectors.toMap(Recipe::getRcpSeq, Function.identity()));

        for (RecipeDto dto : chunk) {
            Recipe recipe = existing.get(dto.getRCP_SEQ());
            if (recipe == null) {
                recipe = new Recipe();
                result.created++;
            } else {
                result.updated++;
            }
            recipe.updateFrom(dto);

            List<RecipeIngredient> ingredients = new ArrayList<>();
            for (RecipeIngredientParser.ParsedIngredient parsed : ingredientParser.parse(dto.getRCP_PARTS_DTLS())) {
                Ingredient linked = master.get(parsed.name());
                RecipeIngredient ingredient = new RecipeIngredient();
                ingredient.setName(parsed.name());
                ingredient.setRawText(truncate(parsed.rawText()));
                ingredient.setIngredient(linked);
                ingredient.setNeedsReview(linked == null); // 마스터에 없는 이름은 레시피 이름 그대로 두고 확인 표시 (D-025)
                ingredients.add(ingredient);

                result.ingredientCount++;
                if (linked != null) {
                    result.linkedCount++;
                } else {
                    result.unlinked.merge(parsed.name(), 1, Integer::sum);
                }
            }
            recipe.replaceIngredients(ingredients);
            recipeRepository.save(recipe);
        }
        return result;
    }

    private static String truncate(String raw) {
        return raw != null && raw.length() > RAW_TEXT_MAX ? raw.substring(0, RAW_TEXT_MAX) : raw;
    }
}
