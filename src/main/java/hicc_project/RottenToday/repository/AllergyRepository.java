package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AllergyRepository extends JpaRepository<Allergy, Long> {

    @Query("SELECT a FROM Allergy a WHERE a.member.id = :memberId")
    List<Allergy> findByMemberId(long memberId);

    Optional<Allergy> findByIngredientId(long ingredientId);

}
