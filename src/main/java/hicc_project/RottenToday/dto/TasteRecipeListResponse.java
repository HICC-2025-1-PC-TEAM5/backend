package hicc_project.RottenToday.dto;

import java.util.List;

public class TasteRecipeListResponse {
    private List<TasteRecipeResponse> recipes;

    public TasteRecipeListResponse(List<TasteRecipeResponse> recipes) {
        this.recipes = recipes;
    }

    public List<TasteRecipeResponse> getRecipes() {
        return recipes;
    }
}
