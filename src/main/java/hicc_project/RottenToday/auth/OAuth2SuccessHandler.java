//package hicc_project.RottenToday.auth;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import hicc_project.RottenToday.jwt.JwtTokenProvider;
//import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
//
//    private final JwtTokenProvider jwtTokenProvider;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Override
//    public void onAuthenticationSuccess(HttpServletRequest request,
//                                        HttpServletResponse response,
//                                        Authentication authentication) throws IOException {
//        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
//        String email = oAuth2User.getAttribute("email");
//
//        // JWT 생성
//        String accessToken = jwtTokenProvider.createAccessToken(email);
//        String refreshToken = jwtTokenProvider.createRefreshToken(email);
//
//        // Refresh Token → HttpOnly Cookie 저장
//        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
//        refreshCookie.setHttpOnly(true);
//        refreshCookie.setSecure(false); // HTTPS 환경에서는 true로 변경
//        refreshCookie.setPath("/");
//        refreshCookie.setMaxAge(60 * 60 * 24 * 14); // 14일
//
//        response.addCookie(refreshCookie);
//
//        // Access Token은 JSON으로 응답
//        Map<String, String> tokenResponse = Map.of(
//                "accessToken", accessToken
//        );
//
//        response.setStatus(HttpServletResponse.SC_OK);
//        response.setContentType("application/json");
//        response.setCharacterEncoding("UTF-8");
//        response.getWriter().write(objectMapper.writeValueAsString(tokenResponse));
//    }
//}
