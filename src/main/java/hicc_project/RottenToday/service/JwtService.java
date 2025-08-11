// src/main/java/hicc_project/RottenToday/service/JwtService.java
package hicc_project.RottenToday.service;

import hicc_project.RottenToday.jwt.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;

    public record TokenPair(
            String accessToken,
            String refreshToken,
            long accessExpiresInSeconds,
            long refreshExpiresInSeconds
    ) {}

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair issue(String subject, Map<String, Object> claims) {
        SecretKey key = key();
        Instant now = Instant.now();

        String access = Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(props.getAccessTtlSeconds())))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refresh = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(props.getRefreshTtlSeconds())))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return new TokenPair(access, refresh, props.getAccessTtlSeconds(), props.getRefreshTtlSeconds());
    }

    /** refresh 토큰 검증 후 Access 재발급 (+ 기본 로테이션으로 새 refresh도 함께 발급) */
    public TokenPair refresh(String refreshToken) {
        SecretKey key = key();

        // 1) refresh 검증(서명/만료)
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(refreshToken);

        Claims rc = jws.getBody();
        String subject = rc.getSubject();

        // (선택) 서버측 무효화/버전 확인 로직이 있다면 여기서 검사

        // 2) Access 재발급 + Refresh 로테이션(보안 권장)
        Instant now = Instant.now();

        String newAccess = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(props.getAccessTtlSeconds())))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String newRefresh = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(props.getRefreshTtlSeconds())))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return new TokenPair(newAccess, newRefresh, props.getAccessTtlSeconds(), props.getRefreshTtlSeconds());
    }
}
