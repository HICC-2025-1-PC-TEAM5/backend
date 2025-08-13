package hicc_project.RottenToday.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.dto.*;
import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.RecipeStep;
import hicc_project.RottenToday.entity.Taste;
import hicc_project.RottenToday.exception.DuplicateEntityException;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
import hicc_project.RottenToday.repository.RecipeStepRepository;
import hicc_project.RottenToday.repository.TasteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final MemberRepository memberRepository;
    private final TasteRepository tasteRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final RecipeStepRepository recipeStepRepository;

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, TasteRepository tasteRepository, MemberRepository memberRepository, ObjectMapper objectMapper, RecipeStepRepository recipeStepRepository) {
        this.recipeRepository = recipeRepository;
        this.tasteRepository = tasteRepository;
        this.memberRepository = memberRepository;
        this.objectMapper = objectMapper;
        this.recipeStepRepository = recipeStepRepository;
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
            throw new IllegalArgumentException("type 변수값으로 '좋아요' 혹은 '싫어요' 입력할 수 있습니다.");
        }
        if (tasteRepository.findByRecipeId(recipeId).isPresent()){
            throw new DuplicateEntityException("이미 해당 레시피를 저장하였습니다.");
        }
        Taste taste = new Taste(appetite, recipe, member);
        tasteRepository.save(taste);

    }

    public List<RecipeResponseDto> getRecipeByIngredients(List<String> ingredients) {
        String ingredintParam = ingredients.stream()
                .map(ing -> "RCP_PARTS_DTLS=" + UriUtils.encode(ing, "UTF-8"))
                .collect(Collectors.joining("&"));
        String url = "http://openapi.foodsafetykorea.go.kr/api/cd8fddb933aa46f18d93/COOKRCP01/json/1/10/" + ingredintParam;
        //String url = "http://openapi.foodsafetykorea.go.kr/api/addc15725715465c947d/COOKRCP01/json/1/10/" + ingredintParam;
        String json = restTemplate.getForObject(url, String.class);
        try {
            CookRecipeResponse response = objectMapper.readValue(json, CookRecipeResponse.class);
            List<RecipeResponseDto> recipeList = response.getCookrcp01().getRow().stream()
                    .map(RecipeResponseDto::from)
                    .collect(Collectors.toList());
            for (RecipeResponseDto dto : recipeList) {
                Recipe recipe = new Recipe(dto);
                List<RecipeStep> steps = dto.getSteps();
                for (RecipeStep step : steps) {
                    step.setRecipe(recipe);
                }
                Recipe save = recipeRepository.save(recipe);
                dto.setId(save.getId());
            }
            return recipeList;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("레시피 파싱 실패", e);
        }
    }
}
