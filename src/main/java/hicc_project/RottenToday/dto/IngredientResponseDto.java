package hicc_project.RottenToday.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IngredientResponseDto {
    private IngredientResponse ingredient;

    public IngredientResponseDto(IngredientResponse ingredient) {
        this.ingredient = ingredient;
    }
}
