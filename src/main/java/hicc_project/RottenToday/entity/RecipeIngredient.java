package hicc_project.RottenToday.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
public class RecipeIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String unit;
    private int quantity;

    // 정규화한 재료 이름 (RecipeIngredientParser.normalize). 추천 매칭에 쓴다 (V2, 인덱스)
    private String name;

    // 레시피 원문 조각 (예: "두부 1/2모")
    @Column(length = 500)
    private String rawText;

    // 재료 마스터에 없어 ingredient가 null이면 true. 나중에 사람이 확인한다 (D-025)
    private boolean needsReview;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Recipe recipe;

    @ManyToOne
    @JoinColumn(name = "ingredient_id")
    @JsonIgnore
    private Ingredient ingredient;
}