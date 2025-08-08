package hicc_project.RottenToday.config;

//import hicc_project.RottenToday.auth.OAuth2SuccessHandler;
import hicc_project.RottenToday.auth.UserDetailsServiceImpl;
import hicc_project.RottenToday.jwt.JwtAuthenticationFilter;
import hicc_project.RottenToday.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
//    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final UserDetailsServiceImpl userDetailsServiceImpl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/api/auth/**",   // 로그인, 토큰 재발급, 로그아웃
                                "/login/oauth2/**", // Google 리다이렉트 (code 반환)
                                "/api/test/public", // 테스트용 공개 API
                                "/oauth2/authorization/google/**"
                        ).permitAll()
                        .anyRequest().authenticated()
  /*              )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)*/
                );

        // JWT 필터 적용
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsServiceImpl);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
