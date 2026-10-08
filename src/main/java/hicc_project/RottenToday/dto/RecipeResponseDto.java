package hicc_project.RottenToday.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import hicc_project.RottenToday.entity.Recipe;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Getter
@Setter
public class RecipeResponseDto {
    private static final Logger log = LoggerFactory.getLogger(RecipeResponseDto.class);
    private Long id;
    private String name;
    private String type;   //"밥"
    private Double kcal;
    private Double protein;
    private Double sodium;
    private Double carbohydrate;
    private String portion = "1인분";
    private Double fat;
    private String ingredients;
    private String image;
    // 조리 단계는 넣지 않는다. 카드에서 쓰지 않고 상세 응답(recipeGuide.steps)에 있다. 넣으면 레시피마다 recipe_step 조회가 생긴다 (D-042)

    // 추천 응답에만 채운다 (레시피 계획 Phase 5, D-019). 다른 응답에서는 빠진다
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer matchedCount;          // 냉장고와 겹치는 재료 수 (양념 제외)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer imminentCount;         // 그중 소비기한 임박 재료 수
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> missingIngredients; // 냉장고에 없고 대신 쓸 재료도 없는 재료 (양념 제외)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> expiredIngredients; // 이 레시피에 쓰이는 소비기한 지난 냉장고 재료 (FE가 확인 안내 표시)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SubstituteDto> substitutes; // 냉장고에 없지만 대신 쓸 수 있는 재료가 있는 레시피 재료 (D-040)



    public RecipeResponseDto() {}

    public RecipeResponseDto(Recipe recipe) {
        this.id = recipe.getId();
        this.name = recipe.getName();
        this.type = recipe.getType();
        this.kcal = recipe.getKcal();
        this.protein = recipe.getProtein();
        this.sodium = recipe.getSodium();
        this.carbohydrate = recipe.getCarbohydrate();
        this.fat = recipe.getFat();
        this.ingredients = recipe.getIngredients();
        this.image = recipe.getImage();
    }
}
