package hicc_project.RottenToday.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.jwt.JwtTokenProvider;
import hicc_project.RottenToday.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2Service {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;


    @Transactional
    public String authenticateWithCode(String code) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        // 1. 토큰 요청
        String tokenResponse = restTemplate.postForObject(
                "https://oauth2.googleapis.com/token",
                Map.of(
                        "code", code,
                        "client_id", clientId,
                        "client_secret", clientSecret,
                        "redirect_uri", redirectUri,
                        "grant_type", "authorization_code"
                ),
                String.class
        );

        JsonNode tokenJson = mapper.readTree(tokenResponse);
        String idToken = tokenJson.get("id_token").asText();

        // 2. ID Token 디코딩 (jwt 형식이므로 . 으로 분리)
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) throw new IllegalArgumentException("Invalid id_token");

        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        JsonNode payloadJson = mapper.readTree(payload);

        String email = payloadJson.get("email").asText();
        String name = payloadJson.get("name").asText();
        String picture = payloadJson.get("picture").asText();

        // 3. 사용자 DB 저장 or 조회
        Optional<Member> optionalUser = memberRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            Member newUser = new Member();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setProfileImage(picture);
            newUser.setProvider("GOOGLE");
            memberRepository.save(newUser);
        }

        // 4. JWT 발급
        return jwtTokenProvider.createAccessToken(email);
    }
}
