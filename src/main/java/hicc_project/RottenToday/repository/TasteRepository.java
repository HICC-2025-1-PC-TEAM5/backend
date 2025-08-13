package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.Taste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TasteRepository extends JpaRepository<Taste, Long> {
    List<Taste> findByMemberId(Long userId);
    Optional<Taste> findByRecipeId(Long recipeId);
    Optional<Taste> findByRecipeIdAndMemberId(Long recipeId, Long memberId);
}
