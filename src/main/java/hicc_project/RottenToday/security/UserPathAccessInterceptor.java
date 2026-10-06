package hicc_project.RottenToday.security;

import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

// /api/users/{userId}/** 의 {userId}가 로그인한 사용자와 같은지 확인한다 (B5, D-012)
// 예외는 GlobalExceptionHandler가 403/401 JSON으로 바꾼다
@Component
public class UserPathAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        @SuppressWarnings("unchecked")
        Map<String, String> vars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars == null || !vars.containsKey("userId")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Member member)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (!String.valueOf(member.getId()).equals(vars.get("userId"))) {
            throw new ForbiddenException("다른 사용자의 데이터에 접근할 수 없습니다.");
        }
        return true;
    }
}
