package hicc_project.RottenToday.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoriteRequestDto {
    private Long recipeId;
    private boolean type;
}
