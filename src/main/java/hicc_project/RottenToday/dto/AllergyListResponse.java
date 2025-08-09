package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Allergy;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AllergyListResponse {
    List<AllergyResponse> allergyList;

    public AllergyListResponse(List<AllergyResponse> allergyList) {
        this.allergyList = allergyList;
    }
}
