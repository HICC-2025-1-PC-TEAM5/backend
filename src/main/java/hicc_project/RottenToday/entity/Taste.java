package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@DiscriminatorValue("TASTE")
public class Taste extends Recipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Appetite type;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;



    protected Taste() {}

    public Taste(Long id, Appetite type, Recipe recipe, Member member){
        this.id = id;
        this.type = type;
        this.member = member;
        this.name = recipe.getName();
        this.info = recipe.getInfo();
        this.image = recipe.getImage();
        this.kcal = recipe.getKcal();
        this.protein = recipe.getProtein();
        this.sodium = recipe.getSodium();
        this.carbohydrate = recipe.getCarbohydrate();
        this.fat = recipe.getFat();
        this.portion = recipe.getPortion();
    }

}
