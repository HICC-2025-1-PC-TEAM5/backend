package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.RefrigeratorIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RefrigeratorIngredientRepository extends JpaRepository<RefrigeratorIngredient, Long> {

    @Query("SELECT r FROM RefrigeratorIngredient r WHERE r.member.id = :memberId")
    List<RefrigeratorIngredient> findByMemberId(Long memberId);
}
