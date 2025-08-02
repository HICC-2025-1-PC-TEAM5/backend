package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.RecipeStep;

import java.util.List;

public class RecipeGuide {
    List<RecipeStep> steps;

    public RecipeGuide(List<RecipeStep> steps) {
        this.steps = steps;
    }

    public List<RecipeStep> getSteps() {
        return steps;
    }

}
