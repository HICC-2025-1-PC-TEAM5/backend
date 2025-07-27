package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Recipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String info;
    private String image;
    private Long kcal;
    private Long protein;
    private Long sodium;
    private Long carbohydrate;
    private Long fat;
    private int portion;

    @OneToMany(mappedBy = "recipe")
    private List<History> cookHistories;

    @OneToMany(mappedBy = "recipe")
    private List<Taste> tastes;

    @OneToMany(mappedBy = "recipe")
    private List<RecipeIngredient> recipeIngredients;
}
