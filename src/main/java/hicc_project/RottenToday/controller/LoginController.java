package hicc_project.RottenToday.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    // Google 로그인 페이지로 redirect (refresh_token 목적: prompt=consent 추가)
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
                        + "&prompt=consent"                  // 매번 동의 받아 refresh_token 받기
                        + "&include_granted_scopes=true";

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(authUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // 콜백: 토큰 교환 → 유저정보 조회 → 두 JSON 모두 반환
    @GetMapping("/google/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDesc
    ) {
        if (error != null) {
            return ResponseEntity.status(400)
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

        // 1) 토큰 교환
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

            // 토큰 JSON 파싱 (access_token / id_token / refresh_token 등)
            Map<String, Object> tokenJson = OM.readValue(tokenRes.getBody(), Map.class);
            String accessToken = (String) tokenJson.get("access_token");

            if (accessToken == null || accessToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"no_access_token\"}");
            }

            // 2) 유저 정보 조회 (권장: Authorization 헤더)
            String userinfoUrl = "https://www.googleapis.com/userinfo/v2/me";
            HttpHeaders uh = new HttpHeaders();
            uh.setBearerAuth(accessToken);
            uh.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<String> userInfoRes =
                    restTemplate.exchange(userinfoUrl, HttpMethod.GET, new HttpEntity<>(uh), String.class);

            // 3) 요청받은 두 JSON 모두 합쳐서 반환
            Map<String, Object> merged = new HashMap<>();
            merged.put("tokenResponse", OM.readValue(tokenRes.getBody(), Map.class));   // 토큰 응답 JSON 그대로
            merged.put("userInfo", OM.readValue(userInfoRes.getBody(), Map.class));     // 유저 정보 JSON 그대로

            String body = OM.writerWithDefaultPrettyPrinter().writeValueAsString(merged);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("oauth2 error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"internal_error\"}");
        }
    }
}
