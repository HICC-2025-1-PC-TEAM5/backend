package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Recipe;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class TasteRecipeDto {

    private Long id;
    private String name;
    private String image;
    private List<String> ingredients;
    private Long kcal;
    private Long protein;
    private Long sodium;
    private Long carbohydrate;
    private Long fat;

    public TasteRecipeDto(Recipe recipe) {
        this.id = recipe.getId();
        this.name = recipe.getName();
        this.image = recipe.getImage();
        this.ingredients =recipe.getRecipeIngredients().stream()
                .map(recipeIngredient -> recipeIngredient.getIngredient().getName())
                .collect(Collectors.toList());
        this.kcal = recipe.getKcal();
        this.protein = recipe.getProtein();
        this.sodium = recipe.getSodium();
        this.carbohydrate = recipe.getCarbohydrate();
        this.fat = recipe.getFat();
    }





}
