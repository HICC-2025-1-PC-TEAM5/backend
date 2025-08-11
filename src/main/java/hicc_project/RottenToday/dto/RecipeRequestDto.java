package hicc_project.RottenToday.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecipeRequestDto {
    private Long recipeId;
    private String type; //Appetite
}
