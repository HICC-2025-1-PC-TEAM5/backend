// src/main/java/hicc_project/RottenToday/controller/AuthController.java
package hicc_project.RottenToday.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.dto.auth.AuthResponseDto;
import hicc_project.RottenToday.service.JwtService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/oauth2")
@RequiredArgsConstructor
public class AuthController {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    private final RestTemplate restTemplate;
    private final JwtService jwtService;
    private static final ObjectMapper OM = new ObjectMapper();

    private static final String REDIRECT_URI = "http://localhost:8080/api/v2/oauth2/google/callback";

    @GetMapping("/google")
    public ResponseEntity<Void> redirectToGoogle(HttpSession session) {
        String state = java.util.UUID.randomUUID().toString();
        session.setAttribute("OAUTH2_STATE", state);

        String authUrl =
                "https://accounts.google.com/o/oauth2/v2/auth"
                        + "?client_id=" + URLEncoder.encode(googleClientId, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                        + "&response_type=code"
                        + "&scope=" + URLEncoder.encode("email profile openid", StandardCharsets.UTF_8)
                        + "&access_type=offline"
                        + "&prompt=consent"
                        + "&include_granted_scopes=true"
                        + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(authUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping(value = "/google/callback", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleGoogleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDesc,
            HttpSession session
    ) {
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", error, "description", errorDesc));
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No authorization code"));
        }

        // state 검증
        String expected = (String) session.getAttribute("OAUTH2_STATE");
        session.removeAttribute("OAUTH2_STATE");
        if (expected == null || !expected.equals(state)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_state"));
        }

        String tokenUri = "https://oauth2.googleapis.com/token";

        // 1) 코드 → 토큰 교환
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", REDIRECT_URI);
        form.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<String> tokenRes =
                    restTemplate.postForEntity(tokenUri, new HttpEntity<>(form, headers), String.class);

            Map<String, Object> tokenJson = OM.readValue(tokenRes.getBody(), Map.class);
            String googleAccessToken = (String) tokenJson.get("access_token");
            if (googleAccessToken == null || googleAccessToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "no_access_token"));
            }

            // 2) 유저 정보
            String userinfoUrl = "https://www.googleapis.com/userinfo/v2/me";
            HttpHeaders uh = new HttpHeaders();
            uh.setBearerAuth(googleAccessToken);
            uh.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<String> userInfoRes =
                    restTemplate.exchange(userinfoUrl, HttpMethod.GET, new HttpEntity<>(uh), String.class);
            Map<String, Object> userInfo = OM.readValue(userInfoRes.getBody(), Map.class);

            // 3) 우리 JWT 발급
            String subject = String.valueOf(userInfo.getOrDefault("id", "unknown"));
            Map<String, Object> claims = Map.of(
                    "provider", "google",
                    "email", userInfo.get("email"),
                    "name",  userInfo.get("name")
            );
            var pair = jwtService.issue(subject, claims);

            // 4) 응답 DTO (구글 RT는 노출 안 함)
            AuthResponseDto body = new AuthResponseDto(
                    new AuthResponseDto.UserDto(
                            (String) userInfo.get("id"),
                            (String) userInfo.get("email"),
                            (String) userInfo.get("name"),
                            (String) userInfo.get("picture")
                    ),
                    new AuthResponseDto.AppJwtDto(
                            pair.accessToken(),
                            pair.accessExpiresInSeconds(),
                            "Bearer"
                    ),
                    new AuthResponseDto.GoogleTokensDto(
                            (String) tokenJson.get("access_token"),
                            ((Number) tokenJson.getOrDefault("expires_in", 0)).longValue(),
                            (String) tokenJson.getOrDefault("token_type", "Bearer"),
                            null, // refreshToken 비노출
                            (String) tokenJson.get("scope"),
                            (String) tokenJson.get("id_token")
                    )
            );

            // 5) 쿠키 세팅: refresh_token 유지, refreshToken(legacy) 제거
            HttpHeaders out = new HttpHeaders();
            out.setCacheControl(CacheControl.noStore());
            out.add("Pragma", "no-cache");

            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", pair.refreshToken())
                    .httpOnly(true)
                    .secure(false)      // HTTPS면 true
                    .sameSite("Lax")    // 크로스도메인이면 "None" + secure(true)
                    .path("/")
                    .maxAge(pair.refreshExpiresInSeconds())
                    .build();
            out.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // legacy 쿠키 제거(있으면 삭제)
            ResponseCookie killLegacy = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            out.add(HttpHeaders.SET_COOKIE, killLegacy.toString());

            return new ResponseEntity<>(body, out, HttpStatus.OK);

        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "internal_error", "message", e.getMessage()));
        }
    }
}
