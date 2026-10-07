package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.dto.RecipeDetailResponse;
import hicc_project.RottenToday.dto.RecipeGuide;
import hicc_project.RottenToday.dto.RecipeResponseDto;
import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.RecipeIngredient;
import hicc_project.RottenToday.entity.RecipeStep;
import hicc_project.RottenToday.exception.GlobalExceptionHandler;
import hicc_project.RottenToday.service.RecipeRecommendService;
import hicc_project.RottenToday.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
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
    private RecipeService recipeService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        recommendService = mock(RecipeRecommendService.class);
        recipeService = mock(RecipeService.class);
        RecipeController controller = new RecipeController();
        ReflectionTestUtils.setField(controller, "recipeService", recipeService);
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

    private static Recipe sampleRecipe() {
        Recipe recipe = new Recipe();
        recipe.setId(7L);
        recipe.setName("두부조림");
        recipe.setType("반찬");
        recipe.setKcal(120.0);
        recipe.setIngredients("두부 1모, 간장 2큰술");
        recipe.setRcpSeq("28");
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setName("두부");
        ingredient.setNeedsReview(true);
        recipe.replaceIngredients(List.of(ingredient));
        RecipeStep step = new RecipeStep(1, "두부를 썬다.", null);
        step.setRecipe(recipe);
        recipe.setRecipeSteps(new ArrayList<>(List.of(step)));
        return recipe;
    }

    @Test
    void 추천_응답은_기존_필드와_추천_필드만_준다() throws Exception { // R15
        RecipeResponseDto dto = new RecipeResponseDto(sampleRecipe());
        dto.setMatchedCount(1);
        dto.setImminentCount(0);
        dto.setMissingIngredients(List.of());
        dto.setExpiredIngredients(List.of("두부"));
        when(recommendService.recommendFromFridge(1L)).thenReturn(List.of(dto));

        mvc.perform(get("/api/users/1/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipe[0].id").value(7))
                .andExpect(jsonPath("$.recipe[0].name").value("두부조림"))
                .andExpect(jsonPath("$.recipe[0].portion").value("1인분"))
                .andExpect(jsonPath("$.recipe[0].ingredients").value("두부 1모, 간장 2큰술"))
                .andExpect(jsonPath("$.recipe[0].steps[0].description").value("두부를 썬다."))
                .andExpect(jsonPath("$.recipe[0].expiredIngredients[0]").value("두부"))
                .andExpect(jsonPath("$.recipe[0].rcpSeq").doesNotExist())
                .andExpect(jsonPath("$.recipe[0].recipeIngredients").doesNotExist());
    }

    @Test
    void 레시피_상세는_화면에_쓰는_값만_주고_내부_필드는_내보내지_않는다() throws Exception { // D-031
        Recipe recipe = sampleRecipe();
        when(recipeService.getRecipeDetail(7L))
                .thenReturn(new RecipeDetailResponse(recipe, new RecipeGuide(recipe.getRecipeSteps())));

        mvc.perform(get("/api/users/1/recipes/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipe.id").value(7))
                .andExpect(jsonPath("$.recipe.name").value("두부조림"))
                .andExpect(jsonPath("$.recipe.type").value("반찬"))
                .andExpect(jsonPath("$.recipe.kcal").value(120.0))
                .andExpect(jsonPath("$.recipe.portion").value("1인분"))
                .andExpect(jsonPath("$.recipe.ingredients").value("두부 1모, 간장 2큰술"))
                .andExpect(jsonPath("$.recipeGuide.steps[0].description").value("두부를 썬다."))
                .andExpect(jsonPath("$.recipe.rcpSeq").doesNotExist())
                .andExpect(jsonPath("$.recipe.recipeIngredients").doesNotExist())
                .andExpect(jsonPath("$.recipe.recipeSteps").doesNotExist())
                .andExpect(jsonPath("$.recipe.used").doesNotExist());
    }
}
