package hicc_project.RottenToday.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hicc_project.RottenToday.dto.RefridgeDto;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class RefrigeratorIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String unit;
    private int quantity;
    private StorageCondition type; //보관 상태

    private LocalDateTime input_date;  //냉장고 반입 시점
    private LocalDateTime expire_date; //소비기한
    private Category category;

    @ManyToOne
    @JsonIgnore
    private Ingredient ingredient;

    @ManyToOne
    @JsonIgnore
    private Member member;

    public RefrigeratorIngredient() {}

    public RefrigeratorIngredient(RefridgeDto refridgeDto) {
        this.name = refridgeDto.getName();
        this.unit = refridgeDto.getUnit();
        this.quantity = refridgeDto.getQuantity();
        if (refridgeDto.getType().equals("실온")) {
            this.type = StorageCondition.NORMAL;
        } else if (refridgeDto.getType().equals("냉동실")) {
            this.type = StorageCondition.FROZEN;
        } else {
            this.type = StorageCondition.REFRIGERATED;
        }
        this.input_date = refridgeDto.getInput_date();
        this.expire_date = refridgeDto.getExpire_date();


        if (refridgeDto.getCategory() == null) {
            this.category = Category.ETC;
        } else if (refridgeDto.getCategory().equals("채소류")) {
            this.category = Category.VEGETABLE;
        } else if (refridgeDto.getCategory().equals("과일류")) {
            this.category = Category.FRUIT;
        } else if (refridgeDto.getCategory().equals("곡류/전분류")) {
            this.category = Category.GRAIN;
        } else if (refridgeDto.getCategory().equals("육류")) {
            this.category = Category.MEAT;
        } else if (refridgeDto.getCategory().equals("어패류")) {
            this.category = Category.SEAFOOD;
        } else if (refridgeDto.getCategory().equals("달걀/난류")) {
            this.category = Category.EGG;
        } else if (refridgeDto.getCategory().equals("유제품")) {
            this.category = Category.DAIRY;
        } else if (refridgeDto.getCategory().equals("두류/콩류")) {
            this.category = Category.BEANS;
        } else if (refridgeDto.getCategory().equals("기름/지방류")) {
            this.category = Category.OIL;
        } else if (refridgeDto.getCategory().equals("조미료/향신료")) {
            this.category = Category.CONDIMENT;
        } else if (refridgeDto.getCategory().equals("가공식품")) {
            this.category = Category.PROCESSED;
        } else if (refridgeDto.getCategory().equals("음료류")) {
            this.category = Category.DRINK;
        } else {
            this.category = Category.ETC;
        }
    }

}
