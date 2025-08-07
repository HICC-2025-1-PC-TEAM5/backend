package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.transaction.Transactional;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByEmail(String email);

    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.email = :email")
    void deleteByEmail(String email);
}
