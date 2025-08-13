package hicc_project.RottenToday.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IngredientListResponse {
    List<IngredientResponse> ingredientList;

    public IngredientListResponse(List<IngredientResponse> ingredientList) {
        this.ingredientList = ingredientList;
    }
}
