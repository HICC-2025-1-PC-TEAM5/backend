package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.RecipeStep;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Getter
@Setter
public class RecipeResponseDto {
    private static final Logger log = LoggerFactory.getLogger(RecipeResponseDto.class);
    private Long id;
    private String name;
    private String type;   //"밥"
    private Double kcal;
    private Double protein;
    private Double sodium;
    private Double carbohydrate;
    private String portion = "1인분";
    private Double fat;
    private String ingredients;
    private String image;
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
        res.setImage(dto.getATT_FILE_NO_MAIN());
        res.setType(dto.getRCP_PAT2());
        res.setSteps(dto.getRecipeSteps());
        return res;
    }

    public RecipeResponseDto() {}

    public RecipeResponseDto(Recipe recipe) {
        this.id = recipe.getId();
        this.name = recipe.getName();
        this.type = recipe.getType();
        this.kcal = recipe.getKcal();
        this.protein = recipe.getProtein();
        this.sodium = recipe.getSodium();
        this.carbohydrate = recipe.getCarbohydrate();
        this.fat = recipe.getFat();
        this.ingredients = recipe.getIngredients();
        this.image = recipe.getImage();
        this.steps = recipe.getRecipeSteps();

    }
}
