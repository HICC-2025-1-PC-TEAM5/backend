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
    private LocalDateTime expire_date;

    @ManyToOne
    private Ingredient ingredient;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;
}
