// src/main/java/hicc_project/RottenToday/dto/auth/AuthResponseDto.java
package hicc_project.RottenToday.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class AuthResponseDto {
    private UserDto user;
    private AppJwtDto tokens;
    private GoogleTokensDto googleTokens; // 구글 토큰도 보여줄 때 유지

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UserDto {
        private String id;
        private String email;
        private String name;
        private String picture;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AppJwtDto {
        private String accessToken;
        private long   expiresIn;   // seconds
        private String tokenType;   // "Bearer"
        // refreshToken 제거 (헤더로만 전달)
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class GoogleTokensDto {
        private String accessToken;
        private long   expiresIn;
        private String tokenType;
        private String refreshToken; // 구글 RT는 그대로 보여줄지 여부는 니가 결정
        private String scope;
        private String idToken;
    }
}
