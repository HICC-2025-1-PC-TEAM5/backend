package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.RefrigeratorIngredient;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RefrigeratorIngredientResponse {
    List<RefridgeDto> refrigeratorIngredient;

    public RefrigeratorIngredientResponse(List<RefridgeDto> refrigeratorIngredient) {
        this.refrigeratorIngredient = refrigeratorIngredient;
    }

}
