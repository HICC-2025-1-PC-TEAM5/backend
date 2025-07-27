package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.Taste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TasteRepository extends JpaRepository<Taste, Long> {
    List<Recipe> findByUserId(Long userId);
}
