package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 로그아웃·로테이션. 지운 행 수를 돌려준다 (동시에 같은 토큰으로 refresh하면 한쪽만 1)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshToken t WHERE t.tokenHash = :tokenHash")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    // 새로 발급할 때 그 회원의 만료된 행을 정리한다
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshToken t WHERE t.memberId = :memberId AND t.expiresAt < :now")
    int deleteExpired(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);
}
