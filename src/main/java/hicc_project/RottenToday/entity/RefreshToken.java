package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

// 발급한 refresh 토큰 (D-037). 로그아웃하면 행을 지워 무효화한다. 토큰 원문은 저장하지 않는다
@Getter
@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected RefreshToken() {}

    public RefreshToken(Long memberId, String tokenHash, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }
}
