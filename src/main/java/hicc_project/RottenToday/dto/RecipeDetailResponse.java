package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Recipe;

import java.util.List;

// GET /api/users/{userId}/recipes/{recipeId} → { recipe, recipeGuide: { steps }, expiredIngredients }
public class RecipeDetailResponse {
    private final RecipeDetailDto recipe;
    private final RecipeGuide recipeGuide;
    // 이 레시피 재료 중 사용자 냉장고에서 기한이 지난 것. 없으면 빈 배열 (D-038)
    private final List<String> expiredIngredients;

    public RecipeDetailResponse(Recipe recipe, RecipeGuide recipeGuide, List<String> expiredIngredients) {
        this.recipe = RecipeDetailDto.from(recipe);
        this.recipeGuide = recipeGuide;
        this.expiredIngredients = expiredIngredients == null ? List.of() : List.copyOf(expiredIngredients);
    }

    public RecipeDetailDto getRecipe() {
        return recipe;
    }

    public RecipeGuide getRecipeGuide() {
        return recipeGuide;
    }

    public List<String> getExpiredIngredients() {
        return expiredIngredients;
    }
}
