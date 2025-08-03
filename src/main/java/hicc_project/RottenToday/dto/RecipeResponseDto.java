package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.RecipeStep;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RecipeResponseDto {
    private String name;
    private String type;
    private Long kcal;
    private Long protein;
    private Long sodium;
    private Long carbohydrate;
    private Long fat;
    private String ingredients;
    private String mainImage;
    private List<RecipeStep> steps;


    public static RecipeResponseDto from(RecipeDto dto) {
        RecipeResponseDto res = new RecipeResponseDto();
        res.setName(dto.getRCP_NM());
        res.setIngredients(dto.getRCP_PARTS_DTLS());
        res.setKcal(dto.getINFO_ENG());
        res.setProtein(dto.getINFO_PRO());
        res.setCarbohydrate(dto.getINFO_CAR());
        res.setSodium(dto.getINFO_NA());
        res.setFat(dto.getINFO_FAT());
        res.setMainImage(dto.getATT_FILE_NO_MAIN());
        res.setType(dto.getRCP_PAT2());
        res.setSteps(dto.getRecipeSteps());
        return res;
    }
}
