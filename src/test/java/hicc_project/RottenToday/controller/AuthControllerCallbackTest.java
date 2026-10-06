package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.service.JwtService;
import hicc_project.RottenToday.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// B9: 구글 userinfo에 일부 값이 없을 때 OAuth 콜백이 NPE로 실패하지 않는다
class AuthControllerCallbackTest {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URI = "https://www.googleapis.com/userinfo/v2/me";
    private static final String REDIRECT_URI = "https://api.cookit.example/api/v2/oauth2/google/callback";

    private MockRestServiceServer server;
    private JwtService jwtService;
    private MemberService memberService;
    private AuthController controller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        jwtService = mock(JwtService.class);
        memberService = mock(MemberService.class);
        controller = new AuthController(restTemplate, jwtService, memberService);
        ReflectionTestUtils.setField(controller, "googleClientId", "client-id");
        ReflectionTestUtils.setField(controller, "googleClientSecret", "client-secret");
        ReflectionTestUtils.setField(controller, "frontendMainUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(controller, "googleRedirectUri", REDIRECT_URI);
        session = new MockHttpSession();
        session.setAttribute("OAUTH2_STATE", "state-1");

        Member member = new Member();
        member.setId(7L);
        when(memberService.upsertGoogleUser(any(), any(), any(), any())).thenReturn(member);
        when(jwtService.issue(anyString(), anyMap()))
                .thenReturn(new JwtService.TokenPair("access", "refresh", 3600, 1209600));
    }

    private void googleReturns(String userinfoJson) {
        server.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"g-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERINFO_URI)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(userinfoJson, MediaType.APPLICATION_JSON));
    }

    @Test
    void 구글_인증_요청의_redirect_uri는_설정값을_쓴다() { // C7
        ResponseEntity<Void> res = controller.redirectToGoogle(new MockHttpSession());

        String location = res.getHeaders().getLocation().toString();
        assertThat(location).contains("redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8))
                .doesNotContain("localhost:8080");
    }

    @Test
    void 코드_교환에도_설정한_redirect_uri를_보내고_로그인_후_URL에_토큰이_없다() { // C7, B17
        server.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8))))
                .andRespond(withSuccess("{\"access_token\":\"g-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERINFO_URI)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"g-1\",\"email\":\"a@example.com\"}", MediaType.APPLICATION_JSON));

        ResponseEntity<?> res = controller.handleGoogleCallback("code", "state-1", null, null, session);

        server.verify();
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(res.getHeaders().getLocation().toString())
                .isEqualTo("http://localhost:5173?from=oauth")
                .doesNotContain("access");
    }

    @Test
    void 이름이_없어도_로그인되고_클레임에서_빠진다() {
        googleReturns("{\"id\":\"g-1\",\"email\":\"a@example.com\"}");

        ResponseEntity<?> res = controller.handleGoogleCallback("code", "state-1", null, null, session);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> claims = ArgumentCaptor.forClass(Map.class);
        verify(jwtService).issue(eq("7"), claims.capture());
        assertThat(claims.getValue()).containsEntry("email", "a@example.com").doesNotContainKey("name");
    }

    @Test
    void 이메일이_없으면_회원을_만들지_않고_502() {
        googleReturns("{\"id\":\"g-1\",\"name\":\"홍길동\"}");

        ResponseEntity<?> res = controller.handleGoogleCallback("code", "state-1", null, null, session);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(res.getBody()).asString().contains("incomplete_userinfo");
        verifyNoInteractions(memberService, jwtService);
    }

    @Test
    void 구글_id가_없으면_회원을_만들지_않고_502() { // 기존 코드는 "unknown"으로 대체해 계정이 합쳐질 수 있었다
        googleReturns("{\"email\":\"a@example.com\",\"name\":\"홍길동\"}");

        ResponseEntity<?> res = controller.handleGoogleCallback("code", "state-1", null, null, session);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        verifyNoInteractions(memberService, jwtService);
    }

    @Test
    void 구글이_error만_보내고_description이_없어도_400() {
        ResponseEntity<?> res = controller.handleGoogleCallback(null, "state-1", "access_denied", null, session);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).asString().contains("access_denied");
    }
}
