package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Entity
@DiscriminatorValue("HISTORY")
public class History extends Recipe {

    private LocalDateTime viewAt;
    private boolean favorite;

    @ManyToOne
    @JoinColumn(name="member_id")
    private Member member;

    public History() {}

    public History(Member member, LocalDateTime time, Recipe recipe) {
        this.member = member;
        this.viewAt = time;
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
