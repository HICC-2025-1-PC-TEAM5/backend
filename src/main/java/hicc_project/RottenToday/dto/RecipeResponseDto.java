package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.RecipeStep;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RecipeResponseDto {
    private String name;
    private String ingredients;
    private String mainImage;
    private List<RecipeStep> steps;

    public static RecipeResponseDto from(RecipeDto dto) {
        RecipeResponseDto res = new RecipeResponseDto();
        res.setName(dto.getRCP_NM());
        res.setIngredients(dto.getRCP_PARTS_DTLS());
        res.setMainImage(dto.getATT_FILE_NO_MAIN());
        res.setSteps(dto.getRecipeSteps());
        return res;
    }
}
