package hicc_project.RottenToday.repository;


import hicc_project.RottenToday.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
