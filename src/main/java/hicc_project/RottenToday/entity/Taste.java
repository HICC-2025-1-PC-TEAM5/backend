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

    public Taste(String type, Recipe recipe, Member member){
        if (type.equals("좋아요")) {
            this.type = Appetite.LIKE;
        } else {
            this.type = Appetite.DISLIKE;
        }
        this.member = member;
        this.recipe = recipe;
    }

}
