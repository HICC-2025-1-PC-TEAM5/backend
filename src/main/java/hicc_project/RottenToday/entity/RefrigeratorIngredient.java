package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class RefrigeratorIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String unit;
    private int quantity;
    private StorageCondition type; //보관 상태

    private LocalDateTime input_date;  //냉장고 반입 시점
    private LocalDateTime expire_date; //소비기한

    @ManyToOne
    private Ingredient ingredient;

    @ManyToOne
    private Member member;

}
