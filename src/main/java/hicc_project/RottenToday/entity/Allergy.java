package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Allergy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    protected Allergy() {}

    public Allergy(Member member, Ingredient ingredient) {
        this.member = member;
        this.ingredient = ingredient;
    }

}
