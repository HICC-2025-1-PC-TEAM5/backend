package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.exception.GlobalExceptionHandler;
import hicc_project.RottenToday.service.RecipeRecommendService;
import hicc_project.RottenToday.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// B6: 추천 API가 예외를 직접 삼키지 않고 GlobalExceptionHandler에 맡기는지, 빈 결과를 200으로 주는지
// C1: 추천은 GET이고 재료는 서버가 냉장고에서 고른다 (D-018)
class RecipeControllerTest {

    private RecipeRecommendService recommendService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        recommendService = mock(RecipeRecommendService.class);
        RecipeController controller = new RecipeController();
        ReflectionTestUtils.setField(controller, "recipeService", mock(RecipeService.class));
        ReflectionTestUtils.setField(controller, "recipeRecommendService", recommendService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 추천_결과가_없으면_200과_빈_목록() throws Exception {
        when(recommendService.recommendFromFridge(1L)).thenReturn(List.of());

        mvc.perform(get("/api/users/1/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipe").isArray())
                .andExpect(jsonPath("$.recipe").isEmpty());
    }

    @Test
    void 기존_POST_추천은_없어졌다() throws Exception {
        mvc.perform(post("/api/users/1/recipes").contentType(MediaType.APPLICATION_JSON).content("[\"감자\"]"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void 서비스_예외는_공통_형식의_500() throws Exception {
        when(recommendService.recommendFromFridge(1L))
                .thenThrow(new RuntimeException("레시피 외부 API 호출 실패: ResourceAccessException"));

        mvc.perform(get("/api/users/1/recipes"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }
}
