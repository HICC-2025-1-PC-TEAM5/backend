package hicc_project.RottenToday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class IngredientDto {

    private Long ingredientId;
    private String name;
    private String category;
    private String subcategory;
}
