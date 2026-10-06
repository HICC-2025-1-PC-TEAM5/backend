package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.RecipeResponseDto;
import hicc_project.RottenToday.dto.RefridgeDto;
import hicc_project.RottenToday.dto.RefrigeratorIngredientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// C1: 냉장고에서 소비기한이 가장 가까운 재료 1개로 추천한다 (D-018)
class RecipeRecommendServiceTest {

    private IngredientService ingredientService;
    private RecipeService recipeService;
    private RecipeRecommendService service;

    @BeforeEach
    void setUp() {
        ingredientService = mock(IngredientService.class);
        recipeService = mock(RecipeService.class);
        service = new RecipeRecommendService(ingredientService, recipeService);
    }

    private static RefridgeDto item(String name, LocalDateTime expire, LocalDateTime input) {
        RefridgeDto dto = new RefridgeDto();
        dto.setName(name);
        dto.setExpire_date(expire);
        dto.setInput_date(input);
        return dto;
    }

    private void fridgeHas(RefridgeDto... items) {
        when(ingredientService.getRefidge(1L))
                .thenReturn(new RefrigeratorIngredientResponse(new ArrayList<>(List.of(items))));
    }

    @Test
    void 소비기한이_가장_가까운_재료로_추천한다() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 7, 0, 0);
        fridgeHas(item("우유", now.plusDays(5), now), item("두부", now.plusDays(1), now), item("감자", now.plusDays(20), now));
        List<RecipeResponseDto> result = List.of(new RecipeResponseDto());
        when(recipeService.getRecipeByIngredients(List.of("두부"), 1L)).thenReturn(result);

        assertThat(service.recommendFromFridge(1L)).isSameAs(result);
        verify(recipeService).getRecipeByIngredients(List.of("두부"), 1L);
    }

    @Test
    void 냉장고가_비어_있으면_외부_API를_부르지_않고_빈_목록() {
        fridgeHas();

        assertThat(service.recommendFromFridge(1L)).isEmpty();
        verifyNoInteractions(recipeService);
    }

    @Test
    void 소비기한이_없는_재료는_뒤로_같으면_먼저_넣은_재료() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 7, 0, 0);

        assertThat(RecipeRecommendService.pickIngredient(List.of(
                item("소금", null, now), item("양파", now.plusDays(3), now))))
                .contains("양파");
        assertThat(RecipeRecommendService.pickIngredient(List.of(
                item("늦게", now.plusDays(3), now.plusHours(1)), item("먼저", now.plusDays(3), now))))
                .contains("먼저");
    }

    @Test
    void 이름이_빈_재료는_고르지_않는다() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 7, 0, 0);

        assertThat(RecipeRecommendService.pickIngredient(List.of(
                item(" ", now, now), item(" 감자 ", now.plusDays(1), now))))
                .contains("감자");
        assertThat(RecipeRecommendService.pickIngredient(List.of(item(null, now, now)))).isEmpty();
    }
}
