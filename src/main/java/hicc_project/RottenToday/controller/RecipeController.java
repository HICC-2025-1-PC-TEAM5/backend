package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.dto.RecipeDetailResponse;
import hicc_project.RottenToday.dto.RecipeRequestDto;
import hicc_project.RottenToday.dto.RecipeResponseDto;
import hicc_project.RottenToday.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RecipeController {

    private RecipeService recipeService;

    @Autowired
    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

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

    @PostMapping("/api/users/recipes")
    public ResponseEntity<List<RecipeResponseDto>> recommendRecipe(@RequestBody List<String> ingredients) {
        List<RecipeResponseDto> recipeByIngredients = recipeService.getRecipeByIngredients(ingredients);
        return ResponseEntity.ok(recipeByIngredients);
    }

}
