package hicc_project.RottenToday.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.dto.RecipeResponseDto;
import hicc_project.RottenToday.exception.NoInputException;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
import hicc_project.RottenToday.repository.RecipeStepRepository;
import hicc_project.RottenToday.repository.TasteRepository;
import hicc_project.RottenToday.service.recipe.RecipeIngredientParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class RecipeServiceTest {

    private static final String API_KEY = "test-secret-key-0123456789";

    private RecipeService recipeService;
    private RecipeRepository recipeRepository;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        recipeRepository = mock(RecipeRepository.class);
        // Spring Boot 기본 설정과 같은 ObjectMapper (모르는 필드 무시)
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        recipeService = new RecipeService(recipeRepository, mock(TasteRepository.class),
                mock(MemberRepository.class), objectMapper, mock(RecipeStepRepository.class), new RecipeIngredientParser(java.util.Map.of()));
        ReflectionTestUtils.setField(recipeService, "foodSafetyBaseUrl", "http://openapi.foodsafetykorea.go.kr/api");
        ReflectionTestUtils.setField(recipeService, "foodSafetyApiKey", API_KEY);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(recipeService, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void 외부_API_연결_실패_시_예외와_로그에_API_키가_없다(CapturedOutput output) {
        server.expect(anything()).andRespond(withException(new ConnectException("Connection refused")));

        Throwable thrown = catchThrowable(() -> recipeService.getRecipeByIngredients(List.of("감자"), 1L));

        assertThat(thrown).isInstanceOf(RuntimeException.class);
        assertThat(fullStackTrace(thrown)).doesNotContain(API_KEY);
        assertThat(output.getAll()).doesNotContain(API_KEY);
    }

    @Test
    void 외부_API_5xx_응답_시_예외와_로그에_API_키가_없다(CapturedOutput output) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        Throwable thrown = catchThrowable(() -> recipeService.getRecipeByIngredients(List.of("감자"), 1L));

        assertThat(thrown).isInstanceOf(RuntimeException.class);
        assertThat(fullStackTrace(thrown)).doesNotContain(API_KEY);
        assertThat(output.getAll()).doesNotContain(API_KEY);
    }

    @Test
    void 외부_API_타임아웃_시_예외와_로그에_API_키가_없다(CapturedOutput output) {
        server.expect(anything()).andRespond(withException(new IOException("Read timed out")));

        Throwable thrown = catchThrowable(() -> recipeService.getRecipeByIngredients(List.of("감자"), 1L));

        assertThat(thrown).isInstanceOf(RuntimeException.class);
        assertThat(fullStackTrace(thrown)).doesNotContain(API_KEY);
        assertThat(output.getAll()).doesNotContain(API_KEY);
    }

    @Test
    void 검색_결과가_0건이면_빈_목록을_반환하고_저장하지_않는다() { // B6
        server.expect(anything()).andRespond(withSuccess(
                "{\"COOKRCP01\":{\"total_count\":\"0\",\"RESULT\":{\"MSG\":\"해당하는 데이터가 없습니다.\",\"CODE\":\"INFO-200\"}}}",
                MediaType.APPLICATION_JSON));

        List<RecipeResponseDto> result = recipeService.getRecipeByIngredients(List.of("없는재료"), 1L);

        assertThat(result).isEmpty();
        verifyNoInteractions(recipeRepository);
    }

    @Test
    void 외부_API가_오류_코드를_주면_예외를_던지고_오류_메시지는_노출하지_않는다() {
        server.expect(anything()).andRespond(withSuccess(
                "{\"COOKRCP01\":{\"total_count\":\"\",\"RESULT\":{\"MSG\":\"인증키가 유효하지 않습니다.\",\"CODE\":\"INFO-100\"}}}",
                MediaType.APPLICATION_JSON));

        Throwable thrown = catchThrowable(() -> recipeService.getRecipeByIngredients(List.of("감자"), 1L));

        assertThat(thrown).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("INFO-100")
                .hasMessageNotContaining("인증키");
        verifyNoInteractions(recipeRepository);
    }

    @Test
    void 재료가_비어_있으면_NoInputException이고_외부_API를_호출하지_않는다() { // B6
        assertThatThrownBy(() -> recipeService.getRecipeByIngredients(List.of(), 1L))
                .isInstanceOf(NoInputException.class);
        assertThatThrownBy(() -> recipeService.getRecipeByIngredients(null, 1L))
                .isInstanceOf(NoInputException.class);
        server.verify(); // 기대한 요청이 없으므로 호출이 0회여야 통과
    }

    // printStackTrace()로 출력될 내용 전체 (cause 체인 포함)
    private static String fullStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
