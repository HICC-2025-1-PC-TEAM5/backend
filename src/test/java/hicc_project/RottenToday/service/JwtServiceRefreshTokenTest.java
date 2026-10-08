package hicc_project.RottenToday.service;

import hicc_project.RottenToday.jwt.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// B28·D-037: refresh 토큰은 DB에 해시로 저장하고, 로그아웃(revoke)·로테이션으로 지운 토큰은 다시 쓸 수 없다
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({JwtService.class, MemberService.class, JwtServiceRefreshTokenTest.Config.class})
class JwtServiceRefreshTokenTest {

    // 테스트 전용 서명 키 (실제 설정값 아님)
    private static final String TEST_SECRET = "test-secret-for-refresh-token-tests-0123456789";

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @TestConfiguration
    static class Config {
        @Bean
        JwtProperties jwtProperties() {
            JwtProperties props = new JwtProperties();
            props.setSecret(TEST_SECRET);
            return props;
        }
    }

    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String memberId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO member (token_version, email, external_id, provider) VALUES (0, 'a@test', 'ext-a', 'google')");
        memberId = String.valueOf(jdbcTemplate.queryForObject("SELECT id FROM member WHERE external_id = 'ext-a'", Long.class));
    }

    private int rows() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refresh_token", Integer.class);
    }

    @Test
    void 발급하면_토큰_원문이_아니라_해시를_저장한다() {
        var pair = jwtService.issue(memberId, Map.of("tver", 0L));

        assertThat(rows()).isEqualTo(1);
        String stored = jdbcTemplate.queryForObject("SELECT token_hash FROM refresh_token", String.class);
        assertThat(stored).hasSize(64).isEqualTo(JwtService.hash(pair.refreshToken())).isNotEqualTo(pair.refreshToken());
    }

    @Test
    void refresh하면_새_토큰으로_바뀌고_이전_토큰은_다시_쓸_수_없다() {
        var first = jwtService.issue(memberId, Map.of("tver", 0L));

        var second = jwtService.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(rows()).isEqualTo(1);
        assertThatThrownBy(() -> jwtService.refresh(first.refreshToken())).isInstanceOf(JwtException.class);
        // 새 토큰은 쓸 수 있다
        assertThat(jwtService.refresh(second.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void 로그아웃한_refresh_토큰은_서명이_맞아도_거부한다() {
        var pair = jwtService.issue(memberId, Map.of("tver", 0L));

        assertThat(jwtService.revoke(pair.refreshToken())).contains(Long.valueOf(memberId));

        assertThat(rows()).isZero();
        assertThatThrownBy(() -> jwtService.refresh(pair.refreshToken())).isInstanceOf(JwtException.class);
    }

    @Test
    void 저장_기록이_없는_예전_refresh_토큰은_거부한다() {
        // D-037 이전 방식: 서명·만료만 맞는 토큰
        Instant now = Instant.now();
        String legacy = Jwts.builder()
                .setSubject(memberId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.refresh(legacy)).isInstanceOf(JwtException.class);
    }

    @Test
    void 같은_초에_두_번_발급해도_토큰이_겹치지_않는다() {
        var a = jwtService.issue(memberId, Map.of("tver", 0L));
        var b = jwtService.issue(memberId, Map.of("tver", 0L));

        assertThat(a.refreshToken()).isNotEqualTo(b.refreshToken());
        assertThat(rows()).isEqualTo(2); // 기기별로 따로 남는다
    }

    @Test
    void 위조되거나_없는_토큰으로_로그아웃해도_오류_없이_빈_값() {
        assertThat(jwtService.revoke(null)).isEmpty();
        assertThat(jwtService.revoke("not-a-jwt")).isEmpty();
    }

    @Test
    void 회원이_지워지면_refresh_토큰_행도_지워진다() {
        jwtService.issue(memberId, Map.of("tver", 0L));

        jdbcTemplate.update("DELETE FROM member WHERE id = ?", Long.valueOf(memberId));

        assertThat(rows()).isZero();
    }
}
