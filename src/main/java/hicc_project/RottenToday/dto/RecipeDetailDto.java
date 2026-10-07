package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Recipe;

/**
 * 레시피 상세 응답의 recipe 부분 (D-031). 엔티티를 그대로 내보내지 않고 화면에 쓰는 값만 담는다.
 * 조리 단계는 recipeGuide.steps로 따로 준다.
 */
public record RecipeDetailDto(Long id, String name, String type, String image, Double kcal, Double protein,
                              Double sodium, Double carbohydrate, Double fat, String portion, String ingredients) {

    public static RecipeDetailDto from(Recipe recipe) {
        return new RecipeDetailDto(recipe.getId(), recipe.getName(), recipe.getType(), recipe.getImage(),
                recipe.getKcal(), recipe.getProtein(), recipe.getSodium(), recipe.getCarbohydrate(), recipe.getFat(),
                recipe.getPortion(), recipe.getIngredients());
    }
}
