package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.RecipeResponseDto;
import hicc_project.RottenToday.dto.SubstituteDto;
import hicc_project.RottenToday.entity.Allergy;
import hicc_project.RottenToday.entity.Appetite;
import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.dto.RefridgeDto;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RecipeIngredientRepository;
import hicc_project.RottenToday.repository.RecipeIngredientRepository.RecipeIngredientName;
import hicc_project.RottenToday.repository.RecipeRepository;
import hicc_project.RottenToday.repository.RecipeRepository.RecipeSummary;
import hicc_project.RottenToday.service.recipe.RecipeIngredientParser;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 냉장고 재료 전체로 로컬 DB 레시피를 추천한다 (레시피 계획 Phase 5, D-019·D-030). 외부 API를 호출하지 않고 저장하지 않는다.
 * 정렬: 임박 재료 일치 수 ↓ → 전체 일치 수 ↓ → 부족 재료 수 ↑ → RCP_SEQ ↑. 일치는 정규화 이름·동의어가 같을 때만, 양념은 일치·부족에서 뺀다.
 * 알레르기 재료(동의어·대체 재료 포함)가 원문에 들어 있거나 싫어요한 레시피는 뺀다. API 계약은 그대로다 (D-018).
 * 냉장고에 없는 레시피 재료라도 대신 쓸 수 있는 재료가 있으면 부족이 아니라 substitutes로 알린다(일치 수에는 넣지 않음, D-040).
 */
@Slf4j
@Service
public class RecipeRecommendService {

    static final int DEFAULT_LIMIT = 15;
    static final int DEFAULT_IMMINENT_DAYS = 3;
    // 거의 모든 레시피에 들어가 순위를 흐리는 양념. 마늘·대파·양파는 냉장고에 사두는 재료라 일치로 센다 (D-030)
    static final String DEFAULT_SEASONINGS = "소금,후추,설탕,간장,식초,참기름,식용유,올리브유,참깨,깨소금,고춧가루,물엿,올리고당,맛술,청주,물";

    private final IngredientService ingredientService;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeRepository recipeRepository;
    private final MemberRepository memberRepository;
    private final RecipeIngredientParser ingredientParser;
    private final Clock clock;
    private final int imminentDays;
    private final int limit;
    private final Set<String> seasonings;

    public RecipeRecommendService(IngredientService ingredientService,
                                  RecipeIngredientRepository recipeIngredientRepository,
                                  RecipeRepository recipeRepository,
                                  MemberRepository memberRepository,
                                  RecipeIngredientParser ingredientParser,
                                  Clock clock,
                                  @Value("${app.recommend.imminent-days:" + DEFAULT_IMMINENT_DAYS + "}") int imminentDays,
                                  @Value("${app.recommend.limit:" + DEFAULT_LIMIT + "}") int limit,
                                  @Value("${app.recommend.seasonings:" + DEFAULT_SEASONINGS + "}") List<String> seasonings) {
        this.ingredientService = ingredientService;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.recipeRepository = recipeRepository;
        this.memberRepository = memberRepository;
        this.ingredientParser = ingredientParser;
        this.clock = clock;
        this.imminentDays = imminentDays;
        this.limit = limit;
        // 사전과 같은 이름으로 맞추고, 양념을 대신하는 재료도 양념으로 본다 (예: 후추 → 후춧가루, 참깨 → 통깨, D-040)
        Set<String> normalized = new HashSet<>();
        for (String s : seasonings) {
            String name = ingredientParser.normalize(s);
            if (name.isEmpty()) continue;
            normalized.add(name);
            normalized.addAll(ingredientParser.substitutesOf(name));
        }
        this.seasonings = Set.copyOf(normalized);
    }

    /** 냉장고 재료의 상태. 같은 이름의 재료가 여럿이면 하나라도 임박이면 임박, 하나라도 지났으면 지난 재료로 본다 */
    private record FridgeNames(Set<String> all, Set<String> imminent, Set<String> expired) {}

    private record Candidate(RecipeSummary recipe, Set<String> matched, int imminentCount, List<String> missing,
                             List<SubstituteDto> substitutes) {}

    /** 레시피 상세의 냉장고 안내: 지난 재료(D-038)와 대체 가능 재료(D-040) */
    public record DetailNotes(List<String> expiredIngredients, List<SubstituteDto> substitutes) {}

    @Transactional(readOnly = true)
    public List<RecipeResponseDto> recommendFromFridge(Long memberId) {
        long start = System.nanoTime();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));

        FridgeNames fridge = classify(ingredientService.getRefidge(memberId).getRefrigeratorIngredient());
        List<RecipeResponseDto> result = fridge.all().isEmpty() ? List.of() : recommend(member, fridge);

        long serviceMs = (System.nanoTime() - start) / 1_000_000;
        // 측정용 (D-010). 외부 호출과 저장은 없다
        log.info("recipe.metrics externalCalls=0 externalMs=0 serviceMs={} returned={} saved=0", serviceMs, result.size());
        return result;
    }

    /**
     * 레시피 상세용 냉장고 안내 (D-038·D-040). 추천과 같은 규칙(정규화·동의어 일치, 양념 제외)이라 추천 카드의 값과 같다.
     * expiredIngredients: 레시피 재료 중 냉장고에서 기한이 지난 것. substitutes: 냉장고에 없지만 대신 쓸 수 있는 재료가 있는 것
     */
    @Transactional(readOnly = true)
    public DetailNotes detailNotesOf(Long memberId, Long recipeId) {
        FridgeNames fridge = classify(ingredientService.getRefidge(memberId).getRefrigeratorIngredient());
        if (fridge.all().isEmpty()) return new DetailNotes(List.of(), List.of());
        List<String> names = recipeIngredientRepository.findNamesByRecipeIdIn(List.of(recipeId)).stream()
                .map(RecipeIngredientName::getName)
                .filter(n -> !seasonings.contains(n))
                .distinct()
                .toList();
        List<String> expired = names.stream().filter(fridge.expired()::contains).toList();
        List<SubstituteDto> subs = substitutesFor(names.stream().filter(n -> !fridge.all().contains(n)).toList(), fridge.all());
        return new DetailNotes(expired, subs);
    }

    // 냉장고에 없는 레시피 재료마다 대신 쓸 수 있는 냉장고 재료를 하나 찾는다 (사전 순서상 첫 번째)
    private List<SubstituteDto> substitutesFor(List<String> missingNames, Set<String> fridgeNames) {
        List<SubstituteDto> result = new ArrayList<>();
        for (String name : missingNames) {
            ingredientParser.substitutesOf(name).stream().filter(fridgeNames::contains).findFirst()
                    .ifPresent(from -> result.add(new SubstituteDto(name, from)));
        }
        return result;
    }

    private FridgeNames classify(List<RefridgeDto> items) {
        LocalDate today = LocalDate.now(clock);
        Set<String> all = new HashSet<>(), imminent = new HashSet<>(), expired = new HashSet<>();
        for (RefridgeDto item : items) {
            String name = ingredientParser.normalize(item.getName());
            if (name.isEmpty() || seasonings.contains(name)) continue;
            all.add(name);
            if (item.getExpire_date() == null) continue;
            LocalDate expire = item.getExpire_date().toLocalDate();
            if (expire.isBefore(today)) {
                expired.add(name); // 상했는지는 판단하지 않고 일반 재료로 센다 (D-019)
            } else if (!expire.isAfter(today.plusDays(imminentDays))) {
                imminent.add(name);
            }
        }
        return new FridgeNames(all, imminent, expired);
    }

    private List<RecipeResponseDto> recommend(Member member, FridgeNames fridge) {
        // 1. 냉장고 재료가 하나라도 들어간 레시피
        Set<Long> candidateIds = recipeIngredientRepository.findNamesByNameIn(fridge.all()).stream()
                .map(RecipeIngredientName::getRecipeId).collect(Collectors.toCollection(HashSet::new)); // 아래에서 싫어요를 빼므로 변경 가능한 Set
        if (candidateIds.isEmpty()) return List.of();

        // 2. 싫어요 레시피 제외 (id로 비교)
        Set<Long> disliked = member.getTastes().stream()
                .filter(t -> t.getType() == Appetite.DISLIKE && t.getRecipe() != null)
                .map(t -> t.getRecipe().getId()).collect(Collectors.toSet());
        candidateIds.removeAll(disliked);

        // 3. 후보별 재료 이름 (양념 제외)
        Map<Long, Set<String>> namesByRecipe = new HashMap<>();
        if (!candidateIds.isEmpty()) {
            for (RecipeIngredientName row : recipeIngredientRepository.findNamesByRecipeIdIn(candidateIds)) {
                if (!seasonings.contains(row.getName())) {
                    namesByRecipe.computeIfAbsent(row.getRecipeId(), id -> new LinkedHashSet<>()).add(row.getName());
                }
            }
        }

        // 4. 알레르기 재료의 모든 이름(동의어·대체 재료 포함, 공백 제외)이 원문에 들어 있으면 제외. 오탐은 허용하고 누락은 막는다 (B22, D-040)
        Set<String> allergyNames = new HashSet<>();
        for (Allergy allergy : member.getAllergies()) {
            if (allergy.getIngredient() == null) continue; // 재료가 지워진 알레르기 행은 건너뛴다
            for (String name : ingredientParser.relatedNamesOf(allergy.getIngredient().getName())) {
                allergyNames.add(name.replaceAll("\\s+", ""));
            }
        }

        List<Candidate> candidates = new ArrayList<>();
        for (RecipeSummary recipe : recipeRepository.findSummariesByIdIn(candidateIds)) {
            Set<String> names = namesByRecipe.getOrDefault(recipe.getId(), Set.of());
            Set<String> matched = names.stream().filter(fridge.all()::contains).collect(Collectors.toCollection(LinkedHashSet::new));
            if (matched.isEmpty()) continue; // 양념만 겹치는 레시피
            String parts = recipe.getIngredients() == null ? "" : recipe.getIngredients().replaceAll("\\s+", "");
            if (allergyNames.stream().anyMatch(parts::contains)) continue;
            int imminentCount = (int) matched.stream().filter(fridge.imminent()::contains).count();
            List<String> notInFridge = names.stream().filter(n -> !fridge.all().contains(n)).toList();
            // 대신 쓸 수 있는 재료가 있으면 부족에서 뺀다 (정렬의 부족 수에도 반영, D-040)
            List<SubstituteDto> subs = substitutesFor(notInFridge, fridge.all());
            Set<String> substituted = subs.stream().map(SubstituteDto::ingredient).collect(Collectors.toSet());
            List<String> missing = notInFridge.stream().filter(n -> !substituted.contains(n)).toList();
            candidates.add(new Candidate(recipe, matched, imminentCount, missing, subs));
        }

        // 5. 정렬 후 상한
        List<Candidate> top = candidates.stream()
                .sorted(Comparator.comparingInt((Candidate c) -> c.imminentCount()).reversed()
                        .thenComparing(Comparator.comparingInt((Candidate c) -> c.matched().size()).reversed())
                        .thenComparingInt(c -> c.missing().size())
                        .thenComparing(c -> c.recipe().getRcpSeq(), RecipeRecommendService::compareRcpSeq))
                .limit(limit)
                .toList();

        // 6. 응답. 순서를 유지한다
        Map<Long, Recipe> recipes = recipeRepository.findAllById(top.stream().map(c -> c.recipe().getId()).toList())
                .stream().collect(Collectors.toMap(Recipe::getId, Function.identity()));
        List<RecipeResponseDto> result = new ArrayList<>();
        for (Candidate c : top) {
            Recipe recipe = recipes.get(c.recipe().getId());
            RecipeResponseDto dto = new RecipeResponseDto(recipe);
            dto.setMatchedCount(c.matched().size());
            dto.setImminentCount(c.imminentCount());
            dto.setMissingIngredients(c.missing());
            dto.setSubstitutes(c.substitutes());
            dto.setExpiredIngredients(c.matched().stream().filter(fridge.expired()::contains).toList());
            result.add(dto);
        }
        return result;
    }

    /** RCP_SEQ는 숫자 문자열이다. 숫자로 비교하고, 숫자가 아니면 문자열로 비교한다 */
    static int compareRcpSeq(String a, String b) {
        if (a == null || b == null) return a == null ? (b == null ? 0 : 1) : -1;
        try {
            return Long.compare(Long.parseLong(a), Long.parseLong(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }
}
