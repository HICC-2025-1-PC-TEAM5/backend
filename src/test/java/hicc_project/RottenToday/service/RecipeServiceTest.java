package hicc_project.RottenToday.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
import hicc_project.RottenToday.repository.RecipeStepRepository;
import hicc_project.RottenToday.repository.TasteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(OutputCaptureExtension.class)
class RecipeServiceTest {

    private static final String API_KEY = "test-secret-key-0123456789";

    private RecipeService recipeService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(mock(RecipeRepository.class), mock(TasteRepository.class),
                mock(MemberRepository.class), new ObjectMapper(), mock(RecipeStepRepository.class));
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

    // printStackTrace()로 출력될 내용 전체 (cause 체인 포함)
    private static String fullStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
