package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.dto.RecipeDetailResponse;
import hicc_project.RottenToday.dto.RecipeRequestDto;
import hicc_project.RottenToday.dto.RecipeResponseDto;
import hicc_project.RottenToday.service.RecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
        log.info("recommendRecipe");
        log.info("ingredients: {}", ingredients);
        log.info("ingredients: {}, {}", ingredients.getFirst(), ingredients.getLast());
        if (ingredients == null) ingredients = new ArrayList<>();
        if (userId == null) return ResponseEntity.badRequest().build();

        List<RecipeResponseDto> recipeByIngredients;
        try {
            recipeByIngredients = recipeService.getRecipeByIngredients(ingredients, userId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        log.info("recipeByIngredients, {}", recipeByIngredients.getFirst().getName());
        return ResponseEntity.ok(Map.of("recipe", recipeByIngredients));
    }




}
