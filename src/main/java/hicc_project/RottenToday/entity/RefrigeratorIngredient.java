package hicc_project.RottenToday.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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
    private User user;

}
