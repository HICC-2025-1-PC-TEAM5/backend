package hicc_project.RottenToday.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.jwt.JwtTokenProvider;
import hicc_project.RottenToday.entity.RefreshToken;
import hicc_project.RottenToday.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // JWT 생성
        String accessToken = jwtTokenProvider.createAccessToken(email);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        // RefreshToken DB에 저장 또는 업데이트
        RefreshToken savedToken = refreshTokenRepository.findById(email)
                .orElse(new RefreshToken(email, refreshToken));
        savedToken.updateToken(refreshToken);
        refreshTokenRepository.save(savedToken);

        // JSON 응답 구성
        Map<String, String> tokenResponse = Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );

        // 응답 설정
        response.setStatus(HttpServletResponse.SC_OK); // 200
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(tokenResponse));
    }
}
