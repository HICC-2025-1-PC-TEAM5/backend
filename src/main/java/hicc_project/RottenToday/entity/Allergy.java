package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Allergy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;


}
