package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllergyRepository extends JpaRepository<Allergy, Long> {
}