package hicc_project.RottenToday.entity;

import hicc_project.RottenToday.dto.RecipeResponseDto;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class Recipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean isUsed = false;

    private String type;
    private String name;
    private String image;
    private Double kcal;
    private Double protein;
    private Double sodium;
    private Double carbohydrate;
    private Double fat;
    private String portion = "1인분";

    @Column(length = 1000)
    private String ingredients;


    @OneToMany(mappedBy = "recipe")
    private List<RecipeIngredient> recipeIngredients = new ArrayList<>();;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeStep> recipeSteps;

    public Recipe() {};

    public Recipe(RecipeResponseDto responseDto) {
        this.name = responseDto.getName();
        this.type = responseDto.getType();
        this.image = responseDto.getImage();
        this.kcal = responseDto.getKcal();
        this.protein = responseDto.getProtein();
        this.sodium = responseDto.getSodium();
        this.carbohydrate = responseDto.getCarbohydrate();
        this.fat = responseDto.getFat();
        this.ingredients = responseDto.getIngredients();
        this.recipeSteps = responseDto.getSteps();

    };
}
