package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.History;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistoryRepository extends JpaRepository<History, Long> {
    List<History> findByMemberId(Long userId);
    Optional<History> findByRecipeId(Long recipeId);
}
