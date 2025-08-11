package hicc_project.RottenToday.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TasteRecipeResponse {
    private List<TasteRecipeDto> recipe;

    public TasteRecipeResponse(List<TasteRecipeDto> recipe) {
        this.recipe = recipe;
    }
}
