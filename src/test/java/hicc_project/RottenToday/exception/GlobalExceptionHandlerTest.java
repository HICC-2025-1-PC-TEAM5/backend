package hicc_project.RottenToday.exception;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private static final String INTERNAL_DETAIL = "GPT API 응답 오류 (500): {\"secret-body\":\"internal\"}";

    @RestController
    static class ThrowingController {
        @GetMapping("/t/no-input") void noInput() { throw new NoInputException("입력값이 들어오지 않았습니다."); }
        @GetMapping("/t/illegal") void illegal() { throw new IllegalArgumentException("지원하지 않는 파일 형식입니다."); }
        @GetMapping("/t/not-found") void notFound() { throw new EntityNotFoundException("해당 냉장고 재료 없음"); }
        @GetMapping("/t/duplicate") void duplicate() { throw new DuplicateEntityException("이미 알러지 등록한 항목입니다."); }
        @GetMapping("/t/jwt") void jwt() { throw new MalformedJwtException("Malformed JWT JSON: {garbage"); }
        @GetMapping("/t/runtime") void runtime() { throw new RuntimeException(INTERNAL_DETAIL); }
        @GetMapping("/t/status") void status() { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "금지"); }
        @PostMapping("/t/body") void body(@RequestBody List<String> body) { }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void NoInputException은_400과_메시지() throws Exception {
        mvc.perform(get("/t/no-input"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("입력값이 들어오지 않았습니다."));
    }

    @Test
    void IllegalArgumentException은_400과_메시지() throws Exception {
        mvc.perform(get("/t/illegal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 파일 형식입니다."));
    }

    @Test
    void EntityNotFoundException은_404와_메시지() throws Exception {
        mvc.perform(get("/t/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("해당 냉장고 재료 없음"));
    }

    @Test
    void DuplicateEntityException은_409와_메시지() throws Exception {
        mvc.perform(get("/t/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("이미 알러지 등록한 항목입니다."));
    }

    @Test
    void 잘못된_JSON_본문은_400이고_파싱_오류가_노출되지_않는다() throws Exception { // B15
        String body = mvc.perform(post("/t/body").contentType(MediaType.APPLICATION_JSON).content("[\"감자\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 본문 형식이 올바르지 않습니다."))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("JSON parse error");
    }

    @Test
    void JWT_예외는_401이고_파싱_오류가_노출되지_않는다() throws Exception { // B16
        String body = mvc.perform(get("/t/jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증 정보가 유효하지 않습니다."))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("Malformed");
    }

    @Test
    void 처리되지_않은_예외는_500_고정_문구이고_상세는_서버_로그에만_남는다(CapturedOutput output) throws Exception { // B8
        String body = mvc.perform(get("/t/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("secret-body");
        assertThat(output.getAll()).contains("secret-body");
    }

    @Test
    void ResponseStatusException은_지정한_상태를_유지한다() throws Exception {
        mvc.perform(get("/t/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("금지"));
    }
}
