package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.RecipeStep;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class RecipeGuide {
    List<RecipeStep> steps;

    public RecipeGuide(List<RecipeStep> steps) {
        this.steps = steps;
    }

    public List<RecipeStep> getSteps() {
        return steps;
    }

}
