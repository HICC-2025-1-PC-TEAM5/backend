package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.CookHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CookHistoryRepository extends JpaRepository<CookHistory, Long> {
}