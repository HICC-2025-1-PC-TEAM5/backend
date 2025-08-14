// src/main/java/hicc_project/RottenToday/controller/AuthController.java
package hicc_project.RottenToday.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.service.JwtService;
import hicc_project.RottenToday.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

    // 프론트 메인 URL (환경으로 오버라이드 가능)
    @Value("${app.frontend.main-url:http://localhost:5173}")
    private String frontendMainUrl;

    // 명시 콜백(있으면 우선), 없으면 요청 도메인/프로토콜로 계산
    @Value("${app.oauth.redirect-uri:}")
    private String redirectUriProp;

    private final RestTemplate restTemplate;
    private final JwtService jwtService;
    private final MemberService memberService;

    private static final ObjectMapper OM = new ObjectMapper();

    private String resolveRedirectUri(HttpServletRequest req) {
        if (redirectUriProp != null && !redirectUriProp.isBlank()) {
            return redirectUriProp.trim();
        }
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v2/oauth2/google/callback")
                .build()
                .toUriString();
    }

    @GetMapping("/google")
    public ResponseEntity<Void> redirectToGoogle(HttpServletRequest req, HttpSession session) {
        String state = java.util.UUID.randomUUID().toString();
        session.setAttribute("OAUTH2_STATE", state);

        String redirectUri = resolveRedirectUri(req);

        String authUrl =
                "https://accounts.google.com/o/oauth2/v2/auth"
                        + "?client_id=" + URLEncoder.encode(googleClientId, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
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

    @GetMapping("/google/callback")
    public ResponseEntity<?> handleGoogleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDesc,
            HttpServletRequest req,
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
        String redirectUri = resolveRedirectUri(req); // 위와 동일해야 함

        // 1) 코드 → 토큰 교환
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", redirectUri);
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

            // 3) 멤버 upsert
            String externalId = String.valueOf(userInfo.getOrDefault("id", "unknown"));
            String email      = (String) userInfo.get("email");
            String name       = (String) userInfo.get("name");
            String picture    = (String) userInfo.get("picture");

            var member = memberService.upsertGoogleUser(externalId, email, name, picture);
            long tver = member.getTokenVersion();

            // 4) 우리 JWT 발급
            String subject = String.valueOf(member.getId());
            Map<String, Object> claims = Map.of(
                    "provider", "google",
                    "email", email,
                    "name",  name,
                    "mid",   member.getId(),
                    "tver",  tver
            );
            var pair = jwtService.issue(subject, claims);

            // 5) refresh 토큰 쿠키 (프론트가 다른 오리진이면 None/secure 권장)
            HttpHeaders out = new HttpHeaders();
            out.setCacheControl(CacheControl.noStore());
            out.add("Pragma", "no-cache");

            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", pair.refreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/")
                    .maxAge(pair.refreshExpiresInSeconds())
                    .build();
            out.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            ResponseCookie killLegacy = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true).secure(true).sameSite("None").path("/").maxAge(0).build();
            out.add(HttpHeaders.SET_COOKIE, killLegacy.toString());

            // 6) 프론트 메인으로 Redirect
            String sep = frontendMainUrl.contains("?") ? "&" : "?";
            out.setLocation(URI.create(frontendMainUrl + sep + "from=oauth"));
            return new ResponseEntity<>(out, HttpStatus.FOUND);

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
