package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.entity.RefreshToken;
import hicc_project.RottenToday.jwt.JwtTokenProvider;
import hicc_project.RottenToday.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ 유효하지 않은 Refresh Token입니다.");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByEmail(email)
                .orElse(null);

        if (stored == null || !stored.getToken().equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ 저장된 토큰과 일치하지 않습니다.");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(email);
        Map<String, String> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ 유효하지 않은 토큰");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        refreshTokenRepository.deleteById(email); // email이 PK인 경우

        return ResponseEntity.ok(" 로그아웃 성공 (RefreshToken 삭제 완료)");
    }
}
