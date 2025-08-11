package hicc_project.RottenToday.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // JWT 기반이므로 세션 미사용
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CORS는 아래 Bean 사용
                .cors(Customizer.withDefaults())
                // JWT 사용 + 쿠키 기반 리프레시 호출할 것이므로 CSRF는 전역 비활성(원하면 특정 경로만 예외 처리해도 OK)
                .csrf(csrf -> csrf.disable())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .authorizeHttpRequests(auth -> auth
                        // OAuth 콜백/로그인, 토큰 재발급/로그아웃은 허용
                        .requestMatchers("/api/v2/oauth2/**").permitAll()
                        .requestMatchers("/api/auth/refresh", "/api/auth/logout").permitAll()
                        // 개발 중엔 전부 허용, 운영 전환 시 authenticated()로 변경
                        .anyRequest().permitAll()
                );
        // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // JWT 필터 있으면 활성화

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 프론트 도메인들: 필요에 맞게 조정
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:8080"   // 서버와 동일 포트면 굳이 필요없지만 유지
        ));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // 쿠키( HttpOnly refresh_token ) 전송 허용
        config.setAllowCredentials(true);
        // (헤더 노출이 필요하면) config.setExposedHeaders(List.of("Authorization"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
