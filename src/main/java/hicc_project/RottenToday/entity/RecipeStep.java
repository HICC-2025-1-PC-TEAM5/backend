package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
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

    public RecipeStep() {}

    public RecipeStep(int stepNum, String description, String image) {
        this.stepNum = stepNum;
        this.description = description;
        this.image = image;
    }
}
