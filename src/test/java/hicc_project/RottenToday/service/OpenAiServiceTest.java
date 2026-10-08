package hicc_project.RottenToday.service;

import hicc_project.RottenToday.config.OpenAiProperties;
import hicc_project.RottenToday.dto.IngredientDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// B10: 세 메서드(사진 URL·영수증 텍스트·Vision 라벨)가 같은 호출·파싱 코드를 쓴다. 실제 OpenAI는 호출하지 않는다
// B29·D-041: 빈 category는 기타, 로그·예외에는 상태 코드와 오류 종류 코드만 (응답 본문·인식 결과는 남기지 않음)
@ExtendWith(OutputCaptureExtension.class)
class OpenAiServiceTest {

    private final List<ClientRequest> requests = new ArrayList<>();

    private OpenAiService service(HttpStatus status, String body) {
        OpenAiProperties props = new OpenAiProperties();
        props.setUrl("https://api.openai.com/v1/chat/completions");
        props.setKey("test-key");
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            requests.add(request);
            return Mono.just(ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        });
        return new OpenAiService(props, builder);
    }

    // 모델 응답 content(재료 JSON 배열)를 Chat API 응답 형태로 감싼다
    private static String chatResponse(String content) {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"" + escaped + "\"}}]}";
    }

    private static final String CONTENT = "[{\"name\":\"깐마늘\",\"category\":\"채소류\",\"subcategory\":\"마늘\"},"
            + "{\"name\":\"신라면\",\"category\":\"가공식품\",\"subcategory\":\"라면\"}]";

    private List<IngredientDto> call(OpenAiService service, String method) {
        Function<OpenAiService, List<IngredientDto>> f = switch (method) {
            case "image" -> s -> s.getimagetoingredient("https://img/fridge.jpg");
            case "receipt" -> s -> s.getChatCompletion("깐마늘\n신라면");
            case "labels" -> s -> s.getpicturetoingredient(List.of("Garlic", "Ramen"));
            default -> throw new IllegalArgumentException(method);
        };
        return f.apply(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image", "receipt", "labels"})
    void 가공식품_음료류는_name을_나머지는_subcategory를_재료_이름으로_쓴다(String method) {
        List<IngredientDto> result = call(service(HttpStatus.OK, chatResponse(CONTENT)), method);

        assertThat(result).extracting(IngredientDto::getName).containsExactly("마늘", "신라면");
        assertThat(result).extracting(IngredientDto::getCategory).containsExactly("채소류", "가공식품");
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).url().getPath()).isEqualTo("/v1/chat/completions");
        assertThat(requests.get(0).headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-key");
    }

    @ParameterizedTest
    @ValueSource(strings = {"image", "receipt", "labels"})
    void API가_4xx를_주면_예외(String method) {
        OpenAiService service = service(HttpStatus.UNAUTHORIZED, "{\"error\":{\"message\":\"bad key\"}}");

        assertThatThrownBy(() -> call(service, method)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void API가_5xx를_주면_예외() {
        OpenAiService service = service(HttpStatus.BAD_GATEWAY, "{}");

        assertThatThrownBy(() -> call(service, "receipt")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void 모델_응답이_JSON_배열이_아니면_일부만_저장하지_않고_예외() { // AGENTS 5.2
        OpenAiService service = service(HttpStatus.OK, chatResponse("식재료를 찾지 못했어요"));

        assertThatThrownBy(() -> call(service, "image")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void category가_비면_기타로_두고_이름이_비면_다른_쪽을_쓴다() {
        String content = "[{\"name\":\"양파\",\"category\":null,\"subcategory\":null},"
                + "{\"name\":null,\"category\":\"가공식품\",\"subcategory\":\"라면\"},"
                + "{\"name\":\"   \",\"category\":\"채소류\",\"subcategory\":\"\"}]"; // 이름이 모두 비면 뺀다
        List<IngredientDto> result = call(service(HttpStatus.OK, chatResponse(content)), "receipt");

        assertThat(result).extracting(IngredientDto::getName).containsExactly("양파", "라면");
        assertThat(result).extracting(IngredientDto::getCategory).containsExactly("기타", "가공식품");
    }

    @Test
    void 오류_응답의_본문은_로그와_예외에_넣지_않고_오류_종류_코드만_남긴다(CapturedOutput output) {
        String body = "{\"error\":{\"message\":\"Incorrect API key provided: sk-test-1234\",\"type\":\"invalid_request_error\",\"code\":\"invalid_api_key\"}}";
        OpenAiService service = service(HttpStatus.UNAUTHORIZED, body);

        assertThatThrownBy(() -> call(service, "receipt"))
                .hasMessage("GPT API 오류 (401, invalid_api_key)");
        assertThat(output).contains("상태코드: 401", "오류코드: invalid_api_key")
                .doesNotContain("Incorrect API key", "sk-test-1234");
    }

    @Test
    void 인식_결과와_파싱_실패_응답은_로그에_남기지_않는다(CapturedOutput output) {
        call(service(HttpStatus.OK, chatResponse(CONTENT)), "image");
        OpenAiService broken = service(HttpStatus.OK, chatResponse("깐마늘이랑 신라면이 보여요"));
        assertThatThrownBy(() -> call(broken, "image")).hasMessage("GPT 응답 JSON 파싱 실패");

        assertThat(output).contains("재료 2개", "응답 길이")
                .doesNotContain("깐마늘", "신라면", "보여요");
    }

    @Test
    void 오류_본문이_JSON이_아니면_unknown() {
        assertThat(OpenAiService.errorCodeOf("<html>bad gateway</html>")).isEqualTo("unknown");
        assertThat(OpenAiService.errorCodeOf("{\"error\":{\"type\":\"server_error\"}}")).isEqualTo("server_error");
    }
}
