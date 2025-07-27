package hicc_project.RottenToday.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Recipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String info;
    private String image;
    private int portion;

    @OneToMany(mappedBy = "recipe")
    private List<CookHistory> cookHistories;

    @OneToMany(mappedBy = "recipe")
    private List<Taste> tastes;

    @OneToMany(mappedBy = "recipe")
    private List<RecipeIngredient> recipeIngredients;
}
