package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.jwt.JwtProperties;
import hicc_project.RottenToday.service.JwtService;
import hicc_project.RottenToday.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// B28·D-037: 로그아웃은 이 기기의 refresh 토큰을 DB에서 지우고, access가 없거나 만료돼도 refresh 토큰의 회원 tokenVersion을 올린다
class SessionControllerLogoutTest {

    private JwtService jwtService;
    private MemberService memberService;
    private SessionController controller;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        memberService = mock(MemberService.class);
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-for-logout-tests-0123456789-abcdef");
        controller = new SessionController(jwtService, memberService, props);
    }

    @Test
    void access_없이_로그아웃해도_refresh_토큰을_지우고_그_회원을_로그아웃한다() {
        when(jwtService.revoke("refresh-cookie")).thenReturn(Optional.of(7L));

        ResponseEntity<?> response = controller.logout(null, "refresh-cookie");

        verify(jwtService).revoke("refresh-cookie");
        verify(memberService).bumpTokenVersion(7L);
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
                .anyMatch(c -> c.startsWith("refresh_token=") && c.contains("Max-Age=0"));
    }

    @Test
    void 쿠키도_access도_없으면_쿠키만_지운다() {
        when(jwtService.revoke(null)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.logout(null, null);

        verifyNoInteractions(memberService);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
