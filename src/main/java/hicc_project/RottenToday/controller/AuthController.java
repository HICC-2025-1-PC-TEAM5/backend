package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.config.GoogleOAuth2Service;
import hicc_project.RottenToday.jwt.JwtTokenProvider;
import hicc_project.RottenToday.repository.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GoogleOAuth2Service googleOAuth2Service;

    // ✅ 1. 구글 로그인 리다이렉션
    @GetMapping("/login/google")
    public void redirectToGoogleLogin(HttpServletResponse response) throws IOException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", "415856406458-38n466ggo853c5ivpacvau5chvm6fleb.apps.googleusercontent.com")
                .queryParam("redirect_uri", "http://localhost:8080/login/oauth2/code/google")
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "offline")
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }

    // ✅ 2. 프론트에서 받은 Google 인증 코드로 로그인 (JWT 발급)
    @PostMapping("/oauth2/code")
    public ResponseEntity<?> loginWithGoogleCode(@RequestBody Map<String, String> body) {
        String code = body.get("code");

        try {
            String jwt = googleOAuth2Service.authenticateWithCode(code);
            return ResponseEntity.ok(Map.of("accessToken", jwt));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ 3. Access Token 재발급 (Refresh Token은 쿠키에서 가져옴)
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request) {
        String refreshToken = getRefreshTokenFromCookie(request);

        if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
            String email = jwtTokenProvider.getEmailFromToken(refreshToken);

            var savedToken = refreshTokenRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            if (!savedToken.getToken().equals(refreshToken)) {
                return ResponseEntity.status(401).body("Invalid refresh token");
            }

            String newAccessToken = jwtTokenProvider.createAccessToken(email);
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } else {
            return ResponseEntity.status(401).body("Invalid or missing refresh token");
        }
    }

    // ✅ 4. 로그아웃 (Refresh Token 삭제 + 쿠키 삭제)
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);

        if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
            String email = jwtTokenProvider.getEmailFromToken(refreshToken);
            refreshTokenRepository.deleteByEmail(email);
        }

        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // HTTPS면 true
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok("Logged out successfully");
    }

    // 쿠키에서 refresh token 추출
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
