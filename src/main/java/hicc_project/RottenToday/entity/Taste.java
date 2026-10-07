package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Taste {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Appetite type;
    private String portion = "1인분";

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;



    protected Taste() {}

    public Taste(String type, Recipe recipe, Member member){
        this.type = Appetite.fromStatus(type);
        this.member = member;
        this.recipe = recipe;
    }
}
