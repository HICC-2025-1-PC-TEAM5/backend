// src/main/java/hicc_project/RottenToday/controller/SessionController.java
package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SessionController {
    private final JwtService jwtService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name="refresh_token", required=false) String refresh) {
        if (refresh == null || refresh.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","no_refresh_cookie"));
        }
        var pair = jwtService.refresh(refresh); // 검증 + 로테이션

        HttpHeaders out = new HttpHeaders();
        out.setCacheControl(CacheControl.noStore());
        out.add("Pragma", "no-cache");

        // 새 refresh_token으로 교체(로테이션)
        ResponseCookie newRefresh = ResponseCookie.from("refresh_token", pair.refreshToken())
                .httpOnly(true)
                .secure(false)      // HTTPS면 true
                .sameSite("Lax")
                .path("/")
                .maxAge(pair.refreshExpiresInSeconds())
                .build();
        out.add(HttpHeaders.SET_COOKIE, newRefresh.toString());

        // legacy 쿠키 제거
        ResponseCookie killLegacy = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        out.add(HttpHeaders.SET_COOKIE, killLegacy.toString());

        return new ResponseEntity<>(
                Map.of("message","OK",
                        "data", Map.of("access", pair.accessToken(), "expiresIn", pair.accessExpiresInSeconds())),
                out, HttpStatus.OK
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        HttpHeaders out = new HttpHeaders();

        // 두 쿠키 모두 삭제
        ResponseCookie expired = ResponseCookie.from("refresh_token", "")
                .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(0).build();
        out.add(HttpHeaders.SET_COOKIE, expired.toString());

        ResponseCookie killLegacy = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(0).build();
        out.add(HttpHeaders.SET_COOKIE, killLegacy.toString());

        return new ResponseEntity<>(Map.of("message","OK"), out, HttpStatus.OK);
    }
}

