package hicc_project.RottenToday.entity;

import jakarta.persistence.*;

@Entity
public class RecipeStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int stepNum;
    private String description;
    private String image;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;
}
