package hicc_project.RottenToday.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final RestTemplate restTemplate;
    private static final ObjectMapper OM = new ObjectMapper();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    // 애플리케이션에서 사용하는 콜백 고정 (네가 쓰던 값)
    private static final String REDIRECT_URI = "http://localhost:8080/api/v2/oauth2/google/callback";

    /** 구글 로그인 페이지 URL 생성 */
    public String buildGoogleAuthUrl() {
        String authBase = "https://accounts.google.com/o/oauth2/v2/auth";
        return authBase
                + "?client_id=" + URLEncoder.encode(googleClientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode("email profile openid", StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&include_granted_scopes=true";
    }

    /** 인가코드로 토큰 교환(JSON 그대로 Map으로 반환) */
    public Map<String, Object> exchangeCodeForToken(String code) {
        String tokenUri = "https://oauth2.googleapis.com/token";

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
            ResponseEntity<String> res =
                    restTemplate.postForEntity(tokenUri, new HttpEntity<>(form, headers), String.class);
            return OM.readValue(res.getBody(), Map.class); // 원본 JSON 유지
        } catch (HttpClientErrorException e) {
            throw e; // 컨트롤러에서 공통 처리
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange code for token", e);
        }
    }

    /** access_token으로 유저정보 조회(JSON 그대로 Map으로 반환) */
    public Map<String, Object> fetchUserInfo(String accessToken) {
        String userinfoUrl = "https://www.googleapis.com/userinfo/v2/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<String> res =
                    restTemplate.exchange(userinfoUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return OM.readValue(res.getBody(), Map.class); // 원본 JSON 유지
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user info", e);
        }
    }
}
