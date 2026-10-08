package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Recipe;

import java.util.List;

// GET /api/users/{userId}/recipes/{recipeId} → { recipe, recipeGuide: { steps }, expiredIngredients, substitutes }
public class RecipeDetailResponse {
    private final RecipeDetailDto recipe;
    private final RecipeGuide recipeGuide;
    // 이 레시피 재료 중 사용자 냉장고에서 기한이 지난 것. 없으면 빈 배열 (D-038)
    private final List<String> expiredIngredients;
    // 냉장고에 없지만 대신 쓸 수 있는 재료. 없으면 빈 배열 (D-040)
    private final List<SubstituteDto> substitutes;

    public RecipeDetailResponse(Recipe recipe, RecipeGuide recipeGuide, List<String> expiredIngredients,
                                List<SubstituteDto> substitutes) {
        this.recipe = RecipeDetailDto.from(recipe);
        this.recipeGuide = recipeGuide;
        this.expiredIngredients = expiredIngredients == null ? List.of() : List.copyOf(expiredIngredients);
        this.substitutes = substitutes == null ? List.of() : List.copyOf(substitutes);
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

    public List<SubstituteDto> getSubstitutes() {
        return substitutes;
    }
}
