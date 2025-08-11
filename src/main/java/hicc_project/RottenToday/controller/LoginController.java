/*
package hicc_project.RottenToday.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/oauth2")
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    private static final ObjectMapper OM = new ObjectMapper();
    private final JwtService jwtService;

    */
/** JSON → Map 변환 (제네릭 경고 제거용) *//*

    private static Map<String, Object> toMap(String json) throws Exception {
        return OM.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    */
/** Google 로그인 페이지로 리다이렉트 *//*

    @GetMapping("/google")
    public ResponseEntity<Void> redirectToGoogle() {
        String redirectUri = "http://localhost:8080/api/v2/oauth2/google/callback";
        String authUrl =
                "https://accounts.google.com/o/oauth2/v2/auth"
                        + "?client_id=" + URLEncoder.encode(googleClientId, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                        + "&response_type=code"
                        + "&scope=" + URLEncoder.encode("email profile openid", StandardCharsets.UTF_8)
                        + "&access_type=offline"
                        + "&prompt=consent"
                        + "&include_granted_scopes=true";

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(authUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    */
/** 콜백: 토큰 교환 → 유저 정보 조회 → 우리 JWT 발급 → 모두 합쳐 응답 *//*

    @GetMapping("/google/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDesc
    ) {
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"" + error + "\",\"description\":\"" + errorDesc + "\"}");
        }
        if (code == null) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"No authorization code.\"}");
        }

        String tokenUri = "https://oauth2.googleapis.com/token";
        String redirectUri = "http://localhost:8080/api/v2/oauth2/google/callback";

        // 1) 구글 토큰 교환
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> tokenRes =
                    restTemplate.postForEntity(tokenUri, new HttpEntity<>(form, headers), String.class);

            Map<String, Object> tokenJson = toMap(tokenRes.getBody());
            String accessToken = (String) tokenJson.get("access_token");
            if (accessToken == null || accessToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"no_access_token\"}");
            }

            // 2) 구글 유저 정보 조회 (Authorization: Bearer)
            String userinfoUrl = "https://www.googleapis.com/userinfo/v2/me";
            HttpHeaders uh = new HttpHeaders();
            uh.setBearerAuth(accessToken);
            uh.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            ResponseEntity<String> userInfoRes =
                    restTemplate.exchange(userinfoUrl, HttpMethod.GET, new HttpEntity<>(uh), String.class);

            Map<String, Object> userInfo = toMap(userInfoRes.getBody());

            // 3) 우리 JWT 발급 (subject는 구글 고유 id 사용 권장)
            String subject = String.valueOf(userInfo.getOrDefault("id", "unknown"));
            Map<String, Object> appClaims = Map.of(
                    "provider", "google",
                    "email", userInfo.get("email"),
                    "name",  userInfo.get("name")
            );
            var pair = jwtService.issue(subject, appClaims);

            // 4) 합쳐서 JSON 반환
            Map<String, Object> merged = new HashMap<>();
            merged.put("tokenResponse", tokenJson);                     // 구글 토큰 응답
            merged.put("userInfo", userInfo);                           // 구글 유저 정보
            merged.put("appJwt", Map.of(                                 // 우리 서버 JWT
                    "accessToken",  pair.accessToken(),
                    "refreshToken", pair.refreshToken(),
                    "tokenType",    "Bearer",
                    "expiresIn",    pair.accessExpiresInSeconds()
            ));

            String body = OM.writerWithDefaultPrettyPrinter().writeValueAsString(merged);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("oauth2 error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"internal_error\"}");
        }
    }
}
*/
