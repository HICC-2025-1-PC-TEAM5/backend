package hicc_project.RottenToday.dto;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;

// 공통 에러 응답 본문 (D-011): error = HTTP 상태 이름, message = 사용자에게 보여 줄 문구
public record ErrorResponse(String error, String message) {

    public static ErrorResponse of(HttpStatusCode status, String message) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return new ErrorResponse(resolved != null ? resolved.name() : String.valueOf(status.value()), message);
    }
}
