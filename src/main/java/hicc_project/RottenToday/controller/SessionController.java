package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.jwt.JwtProperties;
import hicc_project.RottenToday.service.JwtService;
import hicc_project.RottenToday.service.MemberService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SessionController {
    private final JwtService jwtService;
    private final MemberService memberService;
    private final JwtProperties props;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name="refresh_token", required=false) String refresh) {
        if (refresh == null || refresh.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","no_refresh_cookie"));
        }
        var pair = jwtService.refresh(refresh); // 검증 + 로테이션

        HttpHeaders out = new HttpHeaders();
        out.setCacheControl(CacheControl.noStore());
        out.add("Pragma", "no-cache");

        ResponseCookie newRefresh = ResponseCookie.from("refresh_token", pair.refreshToken())
                .httpOnly(true).secure(false).sameSite("Lax").path("/")
                .maxAge(pair.refreshExpiresInSeconds()).build();
        out.add(HttpHeaders.SET_COOKIE, newRefresh.toString());
        ResponseCookie killLegacy = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(0).build();
        out.add(HttpHeaders.SET_COOKIE, killLegacy.toString());

        return new ResponseEntity<>(
                Map.of("message","OK",
                        "data", Map.of("access", pair.accessToken(), "expiresIn", pair.accessExpiresInSeconds())),
                out, HttpStatus.OK
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name="Authorization", required=false) String authorization,
                                    @CookieValue(name="refresh_token", required=false) String refresh) {
        HttpHeaders out = new HttpHeaders();

        // 0) 이 기기의 refresh 토큰을 DB에서 지운다 → 복사해 둔 refresh 토큰도 더는 쓸 수 없다 (D-037, B28)
        Long memberId = jwtService.revoke(refresh).orElse(null);

        // 1) Access 토큰에서 memberId 추출 → tokenVersion++ (이전 access 토큰 모두 무효)
        //    access가 만료돼 읽을 수 없으면 refresh 토큰의 회원으로 올린다
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String access = authorization.substring(7);
            try {
                SecretKey key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
                var claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(access).getBody();
                memberId = Long.parseLong(claims.getSubject());
            } catch (Exception ignore) { /* 토큰 파싱 실패여도 쿠키 삭제는 진행 */ }
        }
        if (memberId != null) {
            memberService.bumpTokenVersion(memberId);
        }

        // 2) refresh 쿠키 삭제 (+ 레거시 삭제)
        ResponseCookie expired = ResponseCookie.from("refresh_token", "")
                .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(0).build();
        out.add(HttpHeaders.SET_COOKIE, expired.toString());

        ResponseCookie killLegacy = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(0).build();
        out.add(HttpHeaders.SET_COOKIE, killLegacy.toString());

        return new ResponseEntity<>(Map.of("message","OK"), out, HttpStatus.OK);
    }
}
