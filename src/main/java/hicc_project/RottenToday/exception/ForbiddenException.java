package hicc_project.RottenToday.exception;

// 로그인은 했지만 다른 사용자의 경로에 접근한 경우 (403, D-012)
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
