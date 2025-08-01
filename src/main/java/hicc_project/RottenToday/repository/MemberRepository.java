package hicc_project.RottenToday.repository;


import hicc_project.RottenToday.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
