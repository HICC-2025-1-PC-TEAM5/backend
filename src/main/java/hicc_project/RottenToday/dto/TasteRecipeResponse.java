package hicc_project.RottenToday.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TasteRecipeResponse {
    private TasteRecipeDto recipe;

    public TasteRecipeResponse(TasteRecipeDto recipe) {
        this.recipe = recipe;
    }
}
