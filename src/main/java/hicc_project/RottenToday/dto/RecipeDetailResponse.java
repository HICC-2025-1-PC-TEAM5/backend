package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Recipe;

// GET /api/users/{userId}/recipes/{recipeId} → { recipe, recipeGuide: { steps } }
public class RecipeDetailResponse {
    private final RecipeDetailDto recipe;
    private final RecipeGuide recipeGuide;

    public RecipeDetailResponse(Recipe recipe, RecipeGuide recipeGuide) {
        this.recipe = RecipeDetailDto.from(recipe);
        this.recipeGuide = recipeGuide;
    }

    public RecipeDetailDto getRecipe() {
        return recipe;
    }

    public RecipeGuide getRecipeGuide() {
        return recipeGuide;
    }
}
