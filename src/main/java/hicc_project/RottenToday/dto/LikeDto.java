package hicc_project.RottenToday.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LikeDto {
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
    public LikeDto(TasteRecipeDto like) {
        this.id = like.getId();
        this.name = like.getName();
        this.image = like.getImage();
        this.portion = like.getPortion();
        this.ingredients = like.getIngredients();
        this.type = like.getType();
        this.kcal = like.getKcal();
        this.protein = like.getProtein();
        this.sodium = like.getSodium();
        this.carbohydrate = like.getCarbohydrate();
        this.fat = like.getFat();
        this.preference = like.getPreference();
    }
}
