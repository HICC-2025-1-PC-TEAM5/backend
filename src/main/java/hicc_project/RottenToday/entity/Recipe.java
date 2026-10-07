package hicc_project.RottenToday.entity;

import hicc_project.RottenToday.dto.RecipeDto;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class Recipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean isUsed = false;

    private String type;
    private String name;
    private String image;
    private Double kcal;
    private Double protein;
    private Double sodium;
    private Double carbohydrate;
    private Double fat;
    private String portion = "1인분";

    @Column(columnDefinition = "TEXT")
    private String ingredients;

    // 식품안전나라 레시피 일련번호 (V2, unique). 로컬 DB 적재 전 요청마다 저장한 레시피는 null
    @Column(name = "rcp_seq", unique = true, length = 20)
    private String rcpSeq;


    // 적재(Phase 4) 때 레시피와 함께 저장·교체한다
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude // @Data끼리 서로 참조하면 toString·hashCode가 무한 재귀한다
    @EqualsAndHashCode.Exclude
    private List<RecipeIngredient> recipeIngredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeStep> recipeSteps;

    public Recipe() {};


    /** CSV 레시피로 내용을 채우거나 갱신한다. 단계는 통째로 교체한다 (레시피 계획 Phase 4) */
    public void updateFrom(RecipeDto dto) {
        this.rcpSeq = dto.getRCP_SEQ();
        this.name = dto.getRCP_NM();
        this.type = dto.getRCP_PAT2();
        this.image = dto.getATT_FILE_NO_MAIN();
        this.kcal = dto.getINFO_ENG();
        this.protein = dto.getINFO_PRO();
        this.sodium = dto.getINFO_NA();
        this.carbohydrate = dto.getINFO_CAR();
        this.fat = dto.getINFO_FAT();
        this.ingredients = dto.getRCP_PARTS_DTLS();
        if (this.recipeSteps == null) {
            this.recipeSteps = new ArrayList<>();
        }
        this.recipeSteps.clear();
        for (RecipeStep step : dto.getRecipeSteps()) {
            step.setRecipe(this);
            this.recipeSteps.add(step);
        }
    }

    /** 파싱한 재료 목록으로 통째로 교체한다 */
    public void replaceIngredients(List<RecipeIngredient> ingredients) {
        this.recipeIngredients.clear();
        for (RecipeIngredient ingredient : ingredients) {
            ingredient.setRecipe(this);
            this.recipeIngredients.add(ingredient);
        }
    }
}
