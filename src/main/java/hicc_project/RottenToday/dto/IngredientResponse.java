package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Ingredient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IngredientResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private String category;
    private boolean allergy;

    public IngredientResponse(Ingredient ingredient) {
        this.id = ingredient.getId();
        this.name = ingredient.getName();
        this.imageUrl = ingredient.getImageUrl();
        this.category = ingredient.getCategory().getType();
    }
}
