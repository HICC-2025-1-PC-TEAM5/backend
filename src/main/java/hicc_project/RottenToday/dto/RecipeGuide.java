package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.RecipeStep;
import lombok.Getter;

import java.util.List;

// 레시피 상세의 조리 단계 { steps: [...] }
@Getter
public class RecipeGuide {
    private final List<RecipeStepDto> steps;

    public RecipeGuide(List<RecipeStep> steps) {
        this.steps = RecipeStepDto.fromAll(steps);
    }
}
