package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // 로컬 DB 적재에서 이미 있는 레시피를 찾는다 (레시피 계획 Phase 2·4)
    Optional<Recipe> findByRcpSeq(String rcpSeq);

    List<Recipe> findAllByRcpSeqIn(Collection<String> rcpSeqs);
}
