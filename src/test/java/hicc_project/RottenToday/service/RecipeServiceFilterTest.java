package hicc_project.RottenToday.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.dto.RecipeResponseDto;
import hicc_project.RottenToday.entity.Allergy;
import hicc_project.RottenToday.entity.Ingredient;
import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.Taste;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
import hicc_project.RottenToday.repository.RecipeStepRepository;
import hicc_project.RottenToday.repository.TasteRepository;
import hicc_project.RottenToday.service.recipe.RecipeIngredientParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// B22: 알레르기 재료의 동의어(계란 ↔ 달걀)로 적힌 레시피도 거른다
// B2: 알레르기·싫어요로 걸린 레시피는 저장만 건너뛰지 않고 응답에서도 뺀다
class RecipeServiceFilterTest {

    private RecipeService recipeService;
    private RecipeRepository recipeRepository;
    private MemberRepository memberRepository;
    private MockRestServiceServer server;
    private Member member;

    @BeforeEach
    void setUp() {
        recipeRepository = mock(RecipeRepository.class);
        memberRepository = mock(MemberRepository.class);
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        RecipeIngredientParser parser = new RecipeIngredientParser(List.of(
                new RecipeIngredientParser.AliasEntry("계란", "달걀", true),
                new RecipeIngredientParser.AliasEntry("삶은달걀", "달걀", false)));
        recipeService = new RecipeService(recipeRepository, mock(TasteRepository.class), memberRepository,
                objectMapper, mock(RecipeStepRepository.class), parser);
        ReflectionTestUtils.setField(recipeService, "foodSafetyBaseUrl", "http://openapi.foodsafetykorea.go.kr/api");
        ReflectionTestUtils.setField(recipeService, "foodSafetyApiKey", "test-key");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(recipeService, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();

        member = new Member();
        member.setId(1L);
        member.setAllergies(new ArrayList<>());
        member.setTastes(new ArrayList<>());
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        AtomicLong ids = new AtomicLong(100);
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
            Recipe r = inv.getArgument(0);
            r.setId(ids.incrementAndGet());
            return r;
        });
    }

    private void allergicTo(String ingredientName) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(ingredientName);
        member.getAllergies().add(new Allergy(member, ingredient));
    }

    private void dislikes(String recipeName) {
        Recipe recipe = new Recipe();
        recipe.setName(recipeName);
        member.getTastes().add(new Taste("싫어요", recipe, member));
    }

    private void externalReturns(String... nameAndParts) {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < nameAndParts.length; i += 2) {
            if (rows.length() > 0) rows.append(',');
            rows.append("{\"RCP_NM\":\"").append(nameAndParts[i])
                    .append("\",\"RCP_PARTS_DTLS\":\"").append(nameAndParts[i + 1]).append("\"}");
        }
        server.expect(anything()).andRespond(withSuccess(
                "{\"COOKRCP01\":{\"total_count\":\"" + nameAndParts.length / 2 + "\",\"row\":[" + rows + "]}}",
                MediaType.APPLICATION_JSON));
    }

    private static List<String> names(List<RecipeResponseDto> recipes) {
        return recipes.stream().map(RecipeResponseDto::getName).toList();
    }

    @Test
    void 알레르기_재료의_동의어로_적힌_레시피도_응답에서_빠진다() { // B22 + B2
        allergicTo("달걀");
        externalReturns(
                "계란찜", "계란 2개, 소금 약간",
                "달걀말이", "삶은 달걀 1개",
                "감자볶음", "감자 1개, 식용유 1큰술");

        List<RecipeResponseDto> result = recipeService.getRecipeByIngredients(List.of("감자"), 1L);

        assertThat(names(result)).containsExactly("감자볶음");
        verify(recipeRepository, times(1)).save(any(Recipe.class)); // 걸러진 레시피는 저장도 안 함
        assertThat(result.get(0).getId()).isNotNull();
    }

    @Test
    void 싫어요한_레시피는_응답에서_빠진다() { // B2
        dislikes("감자국");
        externalReturns(
                "감자국", "감자 1개",
                "감자볶음", "감자 1개");

        List<RecipeResponseDto> result = recipeService.getRecipeByIngredients(List.of("감자"), 1L);

        assertThat(names(result)).containsExactly("감자볶음");
    }

    @Test
    void 알레르기가_없으면_모두_돌려준다() {
        externalReturns(
                "계란찜", "계란 2개",
                "감자볶음", "감자 1개");

        assertThat(names(recipeService.getRecipeByIngredients(List.of("감자"), 1L)))
                .containsExactly("계란찜", "감자볶음");
    }

    @Test
    void 재료_문자열이_없는_레시피도_실패하지_않는다() {
        allergicTo("달걀");
        server.expect(anything()).andRespond(withSuccess(
                "{\"COOKRCP01\":{\"total_count\":\"1\",\"row\":[{\"RCP_NM\":\"물\"}]}}", MediaType.APPLICATION_JSON));

        assertThat(names(recipeService.getRecipeByIngredients(List.of("감자"), 1L))).containsExactly("물");
    }
}
