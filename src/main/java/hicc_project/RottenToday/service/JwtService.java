// src/main/java/hicc_project/RottenToday/service/JwtService.java
package hicc_project.RottenToday.service;

import hicc_project.RottenToday.entity.RefreshToken;
import hicc_project.RottenToday.jwt.JwtProperties;
import hicc_project.RottenToday.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;
    private final MemberService memberService; // ★ tver 조회를 위해 주입
    private final RefreshTokenRepository refreshTokenRepository; // 발급한 refresh 토큰 (D-037)

    public record TokenPair(
            String accessToken,
            String refreshToken,
            long accessExpiresInSeconds,
            long refreshExpiresInSeconds
    ) {}

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public TokenPair issue(String subject, Map<String, Object> claims) {
        SecretKey key = key();
        Instant now = Instant.now();

        String access = Jwts.builder()
                .setSubject(subject)
                .addClaims(claims) // (AuthController에서 tver 포함해 전달)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(props.getAccessTtlSeconds())))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refresh = issueRefresh(subject, now);

        return new TokenPair(access, refresh, props.getAccessTtlSeconds(), props.getRefreshTtlSeconds());
    }

    /**
     * refresh 토큰 검증 후 Access 재발급 (+ 로테이션). Access에 tver 포함.
     * 서명·만료에 더해 DB에 행이 있어야 한다(로그아웃·이미 쓴 토큰·D-037 이전 토큰은 거부 → 401)
     */
    @Transactional
    public TokenPair refresh(String refreshToken) {
        SecretKey key = key();

        // 1) refresh 검증(서명/만료)
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(refreshToken);

        // 2) 발급 기록 확인 + 로테이션: 지운 행이 없으면 무효. 같은 토큰으로 동시에 refresh하면 한쪽만 통과한다
        if (refreshTokenRepository.deleteByTokenHash(hash(refreshToken)) == 0) {
            throw new JwtException("로그인이 만료되었습니다. 다시 로그인해 주세요.");
        }

        String subject = jws.getBody().getSubject(); // subject = memberId (문자열)
        long memberId = Long.parseLong(subject);

        // ★ 현재 tokenVersion 조회 (로그아웃 등으로 변경되었을 수 있음)
        long tver = memberService.getTokenVersion(memberId);

        // 3) Access 재발급 + Refresh 로테이션
        Instant now = Instant.now();

        String newAccess = Jwts.builder()
                .setSubject(subject)
                .claim("tver", tver)         // ★ 필수: 필터 통과용
                .claim("mid", memberId)      // (선택) 편의상 포함
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(props.getAccessTtlSeconds())))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String newRefresh = issueRefresh(subject, now);

        return new TokenPair(newAccess, newRefresh,
                props.getAccessTtlSeconds(), props.getRefreshTtlSeconds());
    }

    /**
     * 로그아웃: 이 refresh 토큰의 행을 지운다(이 기기만). 서명이 맞으면 회원 id를 돌려준다.
     * 만료·위조된 토큰이면 지울 것 없이 빈 값
     */
    @Transactional
    public Optional<Long> revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return Optional.empty();
        refreshTokenRepository.deleteByTokenHash(hash(refreshToken));
        try {
            String subject = Jwts.parserBuilder().setSigningKey(key()).build()
                    .parseClaimsJws(refreshToken).getBody().getSubject();
            return Optional.of(Long.parseLong(subject));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // refresh 토큰을 만들고 해시를 저장한다. 같은 초에 두 번 발급해도 겹치지 않게 무작위 jti를 넣는다
    private String issueRefresh(String subject, Instant now) {
        Instant expiresAt = now.plusSeconds(props.getRefreshTtlSeconds());
        String refresh = Jwts.builder()
                .setSubject(subject)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();

        long memberId = Long.parseLong(subject);
        LocalDateTime nowUtc = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        refreshTokenRepository.deleteExpired(memberId, nowUtc);
        refreshTokenRepository.save(new RefreshToken(memberId, hash(refresh),
                LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC), nowUtc));
        return refresh;
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 쓸 수 없습니다.", e);
        }
    }
}
