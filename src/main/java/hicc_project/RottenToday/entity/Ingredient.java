package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
@Data
@Entity
public class Ingredient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "ingredient")
    private List<Allergy> allergies;

    @OneToMany
    private List<RecipeIngredient> recipeIngredients;

    @OneToMany
    private List<RefrigeratorIngredient> refrigeratorIngredients;
}
