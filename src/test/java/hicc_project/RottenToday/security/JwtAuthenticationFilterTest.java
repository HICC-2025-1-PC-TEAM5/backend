package hicc_project.RottenToday.security;

import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.jwt.JwtProperties;
import hicc_project.RottenToday.service.MemberService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

// B20: 토큰 검증 실패만 401이고, 인증 뒤 컨트롤러에서 난 예외는 401로 바뀌지 않는다
class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";

    private MemberService memberService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        memberService = mock(MemberService.class);
        filter = new JwtAuthenticationFilter(props, memberService);

        Member member = new Member();
        member.setId(1L);
        when(memberService.getTokenVersion(1L)).thenReturn(0L);
        when(memberService.getById(1L)).thenReturn(member);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static String token(long tver) {
        return Jwts.builder()
                .setSubject("1")
                .claim("tver", tver)
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static MockHttpServletRequest request(String bearer) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/1/fridge/image-to-ingredients");
        request.addHeader("Authorization", "Bearer " + bearer);
        return request;
    }

    @Test
    void 유효한_토큰이면_인증하고_다음_필터로_넘긴다() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(token(0)), response, chain);

        verify(chain).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void 인증_뒤_다운스트림_예외는_401로_바꾸지_않고_그대로_던진다() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        doThrow(new IOException("vision 실패")).when(chain).doFilter(any(), any());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request(token(0)), response, chain))
                .isInstanceOf(IOException.class);
        assertThat(response.getStatus()).isNotEqualTo(401);
    }

    @Test
    void 잘못된_토큰이면_401이고_다음_필터로_넘기지_않는다() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("abc"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("invalid_or_expired_token");
        verifyNoInteractions(chain);
    }

    @Test
    void 토큰_버전이_다르면_401() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(token(5)), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("invalid_token_version");
        verifyNoInteractions(chain);
    }
}
