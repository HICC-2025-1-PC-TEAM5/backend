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
    private final MemberService memberService; // ★ tver 조회를 위해 주입

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
                .addClaims(claims) // (AuthController에서 tver 포함해 전달)
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

    /** refresh 토큰 검증 후 Access 재발급 (+ 로테이션). Access에 tver 포함 */
    public TokenPair refresh(String refreshToken) {
        SecretKey key = key();

        // 1) refresh 검증(서명/만료)
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(refreshToken);

        String subject = jws.getBody().getSubject(); // subject = memberId (문자열)
        long memberId = Long.parseLong(subject);

        // ★ 현재 tokenVersion 조회 (로그아웃 등으로 변경되었을 수 있음)
        long tver = memberService.getTokenVersion(memberId);

        // 2) Access 재발급 + Refresh 로테이션
        Instant now = Instant.now();

        String newAccess = Jwts.builder()
                .setSubject(subject)
                .claim("tver", tver)         // ★ 필수: 필터 통과용
                .claim("mid", memberId)      // (선택) 편의상 포함
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

        return new TokenPair(newAccess, newRefresh,
                props.getAccessTtlSeconds(), props.getRefreshTtlSeconds());
    }
}
