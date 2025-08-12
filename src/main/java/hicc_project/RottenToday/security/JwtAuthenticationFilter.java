// src/main/java/hicc_project/RottenToday/security/JwtAuthenticationFilter.java
package hicc_project.RottenToday.security;

import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.jwt.JwtProperties;
import hicc_project.RottenToday.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties props;
    private final MemberService memberService;
    private static final AntPathMatcher PATH = new AntPathMatcher();

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** 공개/예외 경로는 아예 필터를 거치지 않게 함 */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // CORS preflight
        if (HttpMethod.OPTIONS.matches(method)) return true;

        // 허용 경로들(permitAll과 일치하게)
        List<String> white = List.of(
                "/api/v2/oauth2/**",
                "/api/auth/refresh",
                "/api/auth/logout",
                "/api/v2/oauth2/logout-test",
                "/swagger-ui/**",
                "/v3/api-docs/**"
        );
        for (String p : white) {
            if (PATH.match(p, uri)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(bearer) || !bearer.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = bearer.substring(7);
        try {
            // 1) 토큰 파싱/검증
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key())
                    // .setAllowedClockSkewSeconds(60) // 서버시간 오차가 우려되면 주석 해제
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            long memberId = Long.parseLong(claims.getSubject()); // subject=memberId
            Long jwtTver = claims.get("tver", Long.class);
            long currentTver = memberService.getTokenVersion(memberId);

            // 2) tver 불일치 → 즉시 거부 (로그아웃된/무효 토큰)
            if (jwtTver == null || !jwtTver.equals(currentTver)) {
                unauthorized(response, "invalid_token_version");
                return;
            }

            // 3) 인증 주입
            Member member = memberService.getById(memberId);
            var authentication = new UsernamePasswordAuthenticationToken(
                    member, null, Collections.emptyList());
            authentication.setDetails(request);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(request, response);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            unauthorized(response, "invalid_or_expired_token");
        }
    }

    private void unauthorized(HttpServletResponse response, String code) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + code + "\"}");
    }
}
