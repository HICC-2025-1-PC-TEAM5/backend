package hicc_project.RottenToday.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class RecipeStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int stepNum;
    private String description;
    private String image;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    @JsonIgnore
    private Recipe recipe;

    protected RecipeStep() {}

    public RecipeStep(int stepNum, String description, String image) {
        this.stepNum = stepNum;
        this.description = description;
        this.image = image;
    }


}
