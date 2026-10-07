package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // 로컬 DB 적재에서 이미 있는 레시피를 찾는다 (레시피 계획 Phase 2·4)
    Optional<Recipe> findByRcpSeq(String rcpSeq);

    List<Recipe> findAllByRcpSeqIn(Collection<String> rcpSeqs);

    /** 추천 후보의 정렬·알레르기 판단에 필요한 값만 읽는다 (단계·재료 연관은 읽지 않음, 레시피 계획 Phase 5) */
    @Query("SELECT r.id AS id, r.rcpSeq AS rcpSeq, r.ingredients AS ingredients FROM Recipe r WHERE r.id IN :ids")
    List<RecipeSummary> findSummariesByIdIn(@Param("ids") Collection<Long> ids);

    interface RecipeSummary {
        Long getId();
        String getRcpSeq();
        String getIngredients();
    }
}
