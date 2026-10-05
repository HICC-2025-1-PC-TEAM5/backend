package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AllergyRepository extends JpaRepository<Allergy, Long> {

    @Query("SELECT a FROM Allergy a WHERE a.member.id = :memberId")
    List<Allergy> findByMemberId(long memberId);

    // 중복 검사는 회원 범위로 한다 (B4)
    boolean existsByMemberIdAndIngredientId(Long memberId, Long ingredientId);

}
