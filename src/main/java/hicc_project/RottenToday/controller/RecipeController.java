package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.dto.RecipeDetailResponse;
import hicc_project.RottenToday.dto.RecipeRequestDto;
import hicc_project.RottenToday.dto.RecipeResponseDto;
import hicc_project.RottenToday.service.RecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class RecipeController {
    @Autowired
    private RecipeService recipeService;


    @GetMapping("/api/users/{userId}/recipes/{recipeId}")
    public ResponseEntity<RecipeDetailResponse> getRecipeDetail(@PathVariable Long userId, @PathVariable Long recipeId) {
        RecipeDetailResponse recipeDetail = recipeService.getRecipeDetail(recipeId);
        return ResponseEntity.ok(recipeDetail);
    }

    @PatchMapping("/api/users/{userId}/recipes/{recipeId}")
    public ResponseEntity<String> registerFavoriteRecipe(@PathVariable Long userId, @PathVariable Long recipeId, @RequestBody RecipeRequestDto recipeRequestDto) {
        recipeService.addfavorite(userId, recipeId, recipeRequestDto.getType());
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/api/users/{userId}/recipes")
    public ResponseEntity<Map<String, List<RecipeResponseDto>>> recommendRecipe(
            @RequestBody List<String> ingredients,
            @PathVariable Long userId
    ) {
        log.info("recommendRecipe ingredients: {}", ingredients);
        // 입력 검증과 예외 → 상태 코드 변환은 서비스와 GlobalExceptionHandler가 맡는다
        List<RecipeResponseDto> recipeByIngredients = recipeService.getRecipeByIngredients(ingredients, userId);
        return ResponseEntity.ok(Map.of("recipe", recipeByIngredients));
    }




}
