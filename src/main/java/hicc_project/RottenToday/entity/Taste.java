package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Taste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Appetite type;

    private String name;
    private String info;
    private String image;
    private Long kcal;
    private Long protein;
    private Long sodium;
    private Long carbohydrate;
    private Long fat;
    private int portion;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member users;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    public Taste(Long id, Appetite type, Recipe recipe) {
        this.id = id;
        this.type = type;
        this.recipe = recipe;
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
