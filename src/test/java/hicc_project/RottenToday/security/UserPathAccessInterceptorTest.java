package hicc_project.RottenToday.security;

import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// B5: 경로의 {userId}가 로그인한 사용자와 같아야 한다 (D-012)
class UserPathAccessInterceptorTest {

    private final UserPathAccessInterceptor interceptor = new UserPathAccessInterceptor();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(long memberId) {
        Member member = new Member();
        member.setId(memberId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(member, null, Collections.emptyList()));
    }

    private static MockHttpServletRequest requestWithUserId(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (userId != null) {
            request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("userId", userId));
        }
        return request;
    }

    @Test
    void 본인의_userId면_통과() throws Exception {
        loginAs(1L);
        assertThat(interceptor.preHandle(requestWithUserId("1"), new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void 다른_사람의_userId면_403() {
        loginAs(1L);
        assertThatThrownBy(() -> interceptor.preHandle(requestWithUserId("2"), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void 경로에_userId가_없으면_검사하지_않는다() throws Exception {
        loginAs(1L);
        assertThat(interceptor.preHandle(requestWithUserId(null), new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void 로그인하지_않았으면_401() {
        assertThatThrownBy(() -> interceptor.preHandle(requestWithUserId("1"), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }
}
