package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.RefrigeratorIngredient;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RefrigeratorIngredientResponse {
    List<RefrigeratorIngredient> refrigeratorIngredient;

    public RefrigeratorIngredientResponse(List<RefrigeratorIngredient> refrigeratorIngredient) {
        this.refrigeratorIngredient = refrigeratorIngredient;
    }

}
