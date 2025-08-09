package hicc_project.RottenToday.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS 허용 (필요 시 도메인/메서드 제한)
            .cors(Customizer.withDefaults())
            // CSRF: 우리 OAuth 엔드포인트는 제외
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v2/oauth2/**"))
            // 기본 로그인/HTTP Basic 꺼서 간섭 방지
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            // 권한 규칙: OAuth 엔드포인트와 공개 API 허용
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v2/oauth2/**",   // /google, /google/callback
                    "/api/user/**",        // 소셜 로그인 API 등(필요 없으면 제거)
                    "/actuator/**"         // 모니터링(필요 시)
                ).permitAll()
                .anyRequest().permitAll() // 개발 중 전체 허용 (운영 전환 시 authenticated로 변경)
            );

        return http.build();
    }

    // 선택: 로컬 개발용 CORS 널널 설정
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:8080"
        ));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}