package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.Taste;
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
    private String portion = "1인분";
    private String ingredients;
    private String type;
    private Double kcal;
    private Double protein;
    private Double sodium;
    private Double carbohydrate;
    private Double fat;
    private String preference;

    public TasteRecipeDto(Taste taste) {
        this.id = taste.getRecipe().getId();
        this.name = taste.getRecipe().getName();
        this.image = taste.getRecipe().getImage();
        this.type = taste.getRecipe().getType();
        this.ingredients =taste.getRecipe().getIngredients();
        this.kcal = taste.getRecipe().getKcal();
        this.protein = taste.getRecipe().getProtein();
        this.sodium = taste.getRecipe().getSodium();
        this.carbohydrate = taste.getRecipe().getCarbohydrate();
        this.fat = taste.getRecipe().getFat();
        this.preference = taste.getType().getStatus();
    }





}
