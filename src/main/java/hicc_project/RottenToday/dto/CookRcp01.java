package hicc_project.RottenToday.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CookRcp01 {
    private int total_count;
    private List<RecipeDto> row;
}
