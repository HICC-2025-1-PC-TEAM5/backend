package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.Category;
import hicc_project.RottenToday.entity.RefrigeratorIngredient;
import hicc_project.RottenToday.entity.StorageCondition;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RefridgeDto {
    private Long id;

    private String name;
    private String unit;
    private int quantity;
    private String type; //보관 상태

    private LocalDateTime input_date;  //냉장고 반입 시점
    private LocalDateTime expire_date; //소비기한
    private String category;

    public RefridgeDto() {}

    public RefridgeDto(RefrigeratorIngredient refrigeratorIngredient) {
        this.id = refrigeratorIngredient.getId();
        this.name = refrigeratorIngredient.getName();
        this.unit = refrigeratorIngredient.getUnit();
        this.quantity = refrigeratorIngredient.getQuantity();
        this.type = refrigeratorIngredient.getType().getType();
        this.input_date = refrigeratorIngredient.getInput_date();
        this.expire_date = refrigeratorIngredient.getExpire_date();
        this.category = refrigeratorIngredient.getCategory().getType();
    }
}
