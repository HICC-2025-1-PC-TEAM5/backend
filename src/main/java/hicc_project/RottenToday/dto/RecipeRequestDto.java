package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Appetite;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecipeRequestDto {
    private Long recipeId;
    private Appetite type;
}
