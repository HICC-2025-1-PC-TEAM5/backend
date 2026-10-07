package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    /** 레시피 재료 중 이름이 names에 있는 것 (레시피 id, 이름). recipe_ingredient.name 인덱스를 쓴다 (레시피 계획 Phase 5) */
    @Query("SELECT ri.recipe.id AS recipeId, ri.name AS name FROM RecipeIngredient ri WHERE ri.name IN :names")
    List<RecipeIngredientName> findNamesByNameIn(@Param("names") Collection<String> names);

    /** 레시피들의 재료 이름 전체 (일치·부족 재료 계산용) */
    @Query("SELECT ri.recipe.id AS recipeId, ri.name AS name FROM RecipeIngredient ri WHERE ri.recipe.id IN :recipeIds")
    List<RecipeIngredientName> findNamesByRecipeIdIn(@Param("recipeIds") Collection<Long> recipeIds);

    interface RecipeIngredientName {
        Long getRecipeId();
        String getName();
    }
}
