package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Taste {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Appetite type;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;



    protected Taste() {}

    public Taste(Appetite type, Recipe recipe, Member member){
        this.type = type;
        this.member = member;
        this.recipe = recipe;
    }

}
