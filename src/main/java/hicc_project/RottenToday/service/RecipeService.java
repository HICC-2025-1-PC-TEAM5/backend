package hicc_project.RottenToday.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.dto.*;
import hicc_project.RottenToday.entity.*;
import hicc_project.RottenToday.exception.DuplicateEntityException;
import hicc_project.RottenToday.exception.NoInputException;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
import hicc_project.RottenToday.repository.RecipeStepRepository;
import hicc_project.RottenToday.repository.TasteRepository;
import hicc_project.RottenToday.service.recipe.RecipeIngredientParser;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final MemberRepository memberRepository;
    private final TasteRepository tasteRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeIngredientParser ingredientParser; // 알레르기 재료의 동의어를 찾는다 (B22)

    @Value("${foodsafety.api.base-url}")
    private String foodSafetyBaseUrl;

    @Value("${foodsafety.api.key}")
    private String foodSafetyApiKey;

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, TasteRepository tasteRepository, MemberRepository memberRepository, ObjectMapper objectMapper, RecipeStepRepository recipeStepRepository, RecipeIngredientParser ingredientParser) {
        this.recipeRepository = recipeRepository;
        this.tasteRepository = tasteRepository;
        this.memberRepository = memberRepository;
        this.objectMapper = objectMapper;
        this.recipeStepRepository = recipeStepRepository;
        this.ingredientParser = ingredientParser;
    }

    public RecipeDetailResponse getRecipeDetail(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow(() -> new EntityNotFoundException("해당 레시피 존재 x"));
        List<RecipeStep> byRecipeId = recipeStepRepository.findByRecipeId(recipeId);
        RecipeGuide recipeGuide = new RecipeGuide(byRecipeId);
        RecipeDetailResponse response = new RecipeDetailResponse(recipe, recipeGuide);
        return response;
    }

    public void addfavorite(Long userId, Long recipeId, String appetite) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 레시피 존재 x"));
        if (!(appetite.equals("좋아요") || appetite.equals("싫어요"))){
            throw new IllegalArgumentException("type 변수값으로 '좋아요' 혹은 '싫어요'만 입력할 수 있습니다.");
        }
        if (tasteRepository.findByRecipeIdAndMemberId(recipeId, userId).isPresent()){
            throw new DuplicateEntityException("이미 해당 레시피를 저장하였습니다.");
        }
        Taste taste = new Taste(appetite, recipe, member);
        tasteRepository.save(taste);

    }

    public List<RecipeResponseDto> getRecipeByIngredients(List<String> ingredients, Long memberId) {
        long serviceStart = System.nanoTime();
        if (ingredients == null || ingredients.isEmpty()) {
            throw new NoInputException("재료를 하나 이상 입력해 주세요.");
        }
        String ingredintParam = ingredients.stream()
                .map(ing -> "RCP_PARTS_DTLS=" + UriUtils.encode(ing, "UTF-8"))
                .collect(Collectors.joining("&"));
        String url = foodSafetyBaseUrl + "/" + foodSafetyApiKey + "/COOKRCP01/json/1/15/" + ingredintParam;
        long externalStart = System.nanoTime();
        String json;
        try {
            json = restTemplate.getForObject(url, String.class);
        } catch (RestClientException e) {
            // RestTemplate 예외 메시지에는 키가 포함된 요청 URL이 들어가므로 원래 예외를 전달하지 않는다
            String reason = (e instanceof RestClientResponseException re)
                    ? "HTTP " + re.getStatusCode().value()
                    : e.getClass().getSimpleName();
            log.warn("식품안전나라 레시피 조회 실패: {}", reason);
            throw new RuntimeException("레시피 외부 API 호출 실패: " + reason, e.getCause());
        }
        long externalMs = (System.nanoTime() - externalStart) / 1_000_000;
        int saved = 0;
        try {
            CookRecipeResponse response = objectMapper.readValue(json, CookRecipeResponse.class);
            CookRcp01 body = response.getCookrcp01();
            if (body == null) {
                throw new RuntimeException("레시피 외부 API 응답 형식 오류");
            }
            if (body.getRow() == null) {
                // row가 없으면 RESULT.CODE로 0건(INFO-200)과 오류(인증키 오류 등)를 구분한다. MSG는 남기지 않는다
                String code = body.getResult() != null ? body.getResult().getCode() : null;
                if (!"INFO-200".equals(code)) {
                    log.warn("식품안전나라 레시피 조회 오류 코드: {}", code);
                    throw new RuntimeException("레시피 외부 API 오류: " + code);
                }
                long serviceMs = (System.nanoTime() - serviceStart) / 1_000_000;
                log.info("recipe.metrics externalCalls=1 externalMs={} serviceMs={} returned=0 saved=0",
                        externalMs, serviceMs);
                return List.of();
            }
            List<RecipeResponseDto> recipeList = body.getRow().stream()
                    .map(RecipeResponseDto::from)
                    .collect(Collectors.toList());

            Member member = memberRepository.findById(memberId).orElseThrow(() -> new EntityNotFoundException("해당 유저 없음"));
            List<Taste> tastes = member.getTastes();
            // 알레르기 재료의 모든 이름(동의어 포함, 공백 제외). 예: 달걀 → 달걀, 계란, 삶은달걀 … (B22)
            Set<String> allergyNames = new LinkedHashSet<>();
            for (Allergy allergy : member.getAllergies()) {
                for (String name : ingredientParser.namesOf(allergy.getIngredient().getName())) {
                    allergyNames.add(name.replaceAll("\\s+", ""));
                }
            }

            // 알레르기·싫어요에 걸린 레시피는 저장하지 않고 응답에서도 뺀다 (B2)
            List<RecipeResponseDto> result = new ArrayList<>();
            for (RecipeResponseDto dto : recipeList) {
                Recipe recipe = new Recipe(dto);
                int favorite = 0;  //좋아함 1 안좋아함 -1 표시x는 0

                // 원문에 알레르기 재료 이름이 하나라도 들어 있으면 제외한다. 오탐은 허용하고 누락은 막는다 (레시피 계획 결정 5)
                String partsText = recipe.getIngredients() == null ? "" : recipe.getIngredients().replaceAll("\\s+", "");
                if (allergyNames.stream().anyMatch(partsText::contains)) {
                    continue;
                }

                for (Taste taste: tastes) {  //취향 아닌 레시피 포함되면 추천x
                    if (taste.getRecipe().getName().equals(recipe.getName())) {
                        if (taste.getType().getStatus().equals("좋아요")) {
                            favorite = 1;
                        } else if (taste.getType().getStatus().equals("싫어요")) {
                            favorite = -1;
                        }
                    }
                }
                if (favorite < 0) {
                    continue;
                }


                List<RecipeStep> steps = dto.getSteps();
                for (RecipeStep step : steps) {
                    step.setRecipe(recipe);
                }
                Recipe save = recipeRepository.save(recipe);
                dto.setId(save.getId());
                saved++;
                result.add(dto);
            }
            log.info("recipe : {}", result);
            // 측정용 (D-010). URL에 키가 있으므로 URL은 남기지 않는다
            long serviceMs = (System.nanoTime() - serviceStart) / 1_000_000;
            log.info("recipe.metrics externalCalls=1 externalMs={} serviceMs={} returned={} saved={}",
                    externalMs, serviceMs, result.size(), saved);
            return result;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("레시피 파싱 실패", e);
        }
    }
}
