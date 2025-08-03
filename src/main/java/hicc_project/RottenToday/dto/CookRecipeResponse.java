package hicc_project.RottenToday.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CookRecipeResponse {
    @JsonProperty("COOKRCP01")
    private CookRcp01 cookrcp01;
}
