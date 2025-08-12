package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.History;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class HistoryDto {
    private Long id;
    private Long recipeId;
    private LocalDateTime viewAt;
    private boolean favorite;
    private String type;
    private String name;
    private String image;
    private Double kcal;
    private Double protein;
    private Double sodium;
    private Double carbohydrate;
    private Double fat;
    private String portion = "1인분";
    private String ingredients;

    public HistoryDto(History history) {
        this.id = history.getId();
        this.recipeId = history.getRecipe().getId();
        this.viewAt = history.getViewAt();
        this.favorite = history.isFavorite();
        this.type = history.getRecipe().getType();
        this.name = history.getRecipe().getName();
        this.image = history.getRecipe().getImage();
        this.kcal = history.getRecipe().getKcal();
        this.protein = history.getRecipe().getProtein();
        this.sodium = history.getRecipe().getSodium();
        this.carbohydrate = history.getRecipe().getCarbohydrate();
        this.fat = history.getRecipe().getFat();
        this.ingredients = history.getRecipe().getIngredients();
    }
}
