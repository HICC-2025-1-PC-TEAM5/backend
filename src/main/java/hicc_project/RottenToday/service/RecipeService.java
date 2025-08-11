package hicc_project.RottenToday.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.dto.*;
import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.Taste;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
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

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, TasteRepository tasteRepository, MemberRepository memberRepository, ObjectMapper objectMapper) {
        this.recipeRepository = recipeRepository;
        this.tasteRepository = tasteRepository;
        this.memberRepository = memberRepository;
        this.objectMapper = objectMapper;
    }

    public RecipeDetailResponse getRecipeDetail(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow(() -> new EntityNotFoundException("해당 레시피 존재 x"));
        RecipeGuide recipeGuide = new RecipeGuide(recipe.getRecipeSteps());
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
        Taste taste = new Taste(appetite, recipe, member);
        tasteRepository.save(taste);

    }

    public List<RecipeResponseDto> getRecipeByIngredients(List<String> ingredients) {
        String ingredintParam = ingredients.stream()
                .map(ing -> "RCP_PARTS_DTLS=" + UriUtils.encode(ing, "UTF-8"))
                .collect(Collectors.joining("&"));
        String url = "http://openapi.foodsafetykorea.go.kr/api/addc15725715465c947d/COOKRCP01/json/1/10/" + ingredintParam;

        String json = restTemplate.getForObject(url, String.class);
        try {
            CookRecipeResponse response = objectMapper.readValue(json, CookRecipeResponse.class);
            return response.getCookrcp01().getRow().stream()
                    .map(RecipeResponseDto::from)
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("레시피 파싱 실패", e);
        }
    }
}
