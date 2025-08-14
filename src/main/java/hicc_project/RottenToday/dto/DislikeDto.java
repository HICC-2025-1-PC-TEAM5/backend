package hicc_project.RottenToday.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DislikeDto {
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

    public DislikeDto(TasteRecipeDto dislike){
        this.id = dislike.getId();
        this.name = dislike.getName();
        this.image = dislike.getImage();
        this.portion = dislike.getPortion();
        this.ingredients = dislike.getIngredients();
        this.type = dislike.getType();
        this.kcal = dislike.getKcal();
        this.protein = dislike.getProtein();
        this.sodium = dislike.getSodium();
        this.carbohydrate = dislike.getCarbohydrate();
        this.fat = dislike.getFat();
        this.preference = dislike.getPreference();
    }
}
