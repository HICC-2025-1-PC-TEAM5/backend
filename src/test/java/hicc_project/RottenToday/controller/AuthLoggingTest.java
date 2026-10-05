package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.jwt.JwtProperties;
import hicc_project.RottenToday.service.JwtService;
import hicc_project.RottenToday.service.MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// 인증 흐름에서 토큰·개인정보가 콘솔/로그에 남지 않는지 확인한다 (B11)
@ExtendWith(OutputCaptureExtension.class)
class AuthLoggingTest {

    private static final String ACCESS = "access-token-value-abc123";
    private static final String NEW_REFRESH = "new-refresh-token-value-def456";
    private static final String OLD_REFRESH = "old-refresh-token-value-ghi789";

    @Test
    void refresh_성공_시_토큰이_출력되지_않는다(CapturedOutput output) {
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.refresh(OLD_REFRESH))
                .thenReturn(new JwtService.TokenPair(ACCESS, NEW_REFRESH, 3600, 1209600));
        SessionController controller = new SessionController(jwtService, mock(MemberService.class), new JwtProperties());

        ResponseEntity<?> response = controller.refresh(OLD_REFRESH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(output.getAll()).doesNotContain(ACCESS, NEW_REFRESH, OLD_REFRESH);
    }

    @Test
    void refresh_쿠키가_없으면_401() {
        SessionController controller = new SessionController(mock(JwtService.class), mock(MemberService.class), new JwtProperties());

        ResponseEntity<?> response = controller.refresh(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 내_정보_조회_시_이메일이_출력되지_않는다(CapturedOutput output) {
        Member member = new Member();
        member.setId(1L);
        member.setEmail("someone@example.com");
        member.setName("홍길동");
        member.setPicture("http://example.com/p.png");
        UserController controller = new UserController();

        ResponseEntity<?> response = controller.getMyInfo(member);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(output.getAll()).doesNotContain("someone@example.com", "홍길동");
    }
}
