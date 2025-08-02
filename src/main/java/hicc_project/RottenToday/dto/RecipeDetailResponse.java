package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.RecipeStep;

import java.util.List;

public class RecipeDetailResponse {
    private final Recipe recipe;
    private final RecipeGuide recipeGuide;

    public RecipeDetailResponse(Recipe recipe, RecipeGuide recipeGuide) {
        this.recipe = recipe;
        this.recipeGuide = recipeGuide;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public RecipeGuide getRecipeGuide() {
        return recipeGuide;
    }
}
