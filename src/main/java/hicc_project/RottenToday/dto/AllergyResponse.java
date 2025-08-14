package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Allergy;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AllergyResponse {
    private Long allergyId;
    private String name;
    private String category;
    private String imageUrl;

    public AllergyResponse(Allergy allergy) {
        this.allergyId = allergy.getId();
        this.name = allergy.getIngredient().getName();
        this.category = allergy.getIngredient().getCategory().getType();
        if (allergy.getIngredient().getImageUrl() != null) {
            this.imageUrl = allergy.getIngredient().getImageUrl();
        }
    }
}
