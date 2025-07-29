package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
@Entity
public class Ingredient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "ingredient")
    private List<Allergy> allergies = new ArrayList<>();

    @OneToMany
    private List<RecipeIngredient> recipeIngredients = new ArrayList<>();

    @OneToMany
    private List<RefrigeratorIngredient> refrigeratorIngredients = new ArrayList<>();
}
