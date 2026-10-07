package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.RecipeResponseDto;
import hicc_project.RottenToday.entity.*;
import hicc_project.RottenToday.repository.*;
import hicc_project.RottenToday.service.recipe.RecipeIngredientParser;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 레시피 계획 Phase 5: 냉장고 재료 전체로 로컬 DB 레시피를 추천한다 (D-019·D-030). 실제 MySQL에서 쿼리까지 확인한다
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class RecipeRecommendServiceTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

    @Autowired private TestEntityManager em;
    @Autowired private RefrigeratorIngredientRepository fridgeRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private RecipeIngredientRepository recipeIngredientRepository;

    private RecipeRecommendService service;
    private Member member;

    @BeforeEach
    void setUp() {
        RecipeIngredientParser parser = new RecipeIngredientParser(List.of(
                new RecipeIngredientParser.AliasEntry("계란", "달걀", true)));
        IngredientService ingredientService = new IngredientService(fridgeRepository, ingredientRepository, memberRepository, parser);
        Clock clock = Clock.fixed(TODAY.atTime(9, 0).atZone(SEOUL).toInstant(), SEOUL);
        service = new RecipeRecommendService(ingredientService, recipeIngredientRepository, recipeRepository, memberRepository,
                parser, clock, 3, 15, List.of(RecipeRecommendService.DEFAULT_SEASONINGS.split(",")));

        member = em.persist(Member.builder().email("t@test").externalId("t").provider("google").build());
    }

    private Recipe recipe(String seq, String name, String... ingredientNames) {
        Recipe recipe = new Recipe();
        recipe.setRcpSeq(seq);
        recipe.setName(name);
        recipe.setIngredients(String.join(", ", ingredientNames));
        List<RecipeIngredient> ingredients = new ArrayList<>();
        for (String n : ingredientNames) {
            RecipeIngredient ri = new RecipeIngredient();
            ri.setName(n);
            ri.setRawText(n);
            ingredients.add(ri);
        }
        recipe.replaceIngredients(ingredients);
        return em.persist(recipe);
    }

    /** daysLeft: 오늘부터 소비기한까지 남은 일수. null이면 소비기한 없음 */
    private void fridge(String name, Integer daysLeft) {
        RefrigeratorIngredient item = new RefrigeratorIngredient();
        item.setName(name);
        item.setQuantity(1);
        item.setUnit("개");
        item.setType(StorageCondition.REFRIGERATED);
        item.setCategory(Category.ETC);
        item.setMember(member);
        item.setInput_date(TODAY.atStartOfDay());
        item.setExpire_date(daysLeft == null ? null : TODAY.plusDays(daysLeft).atStartOfDay());
        em.persist(item);
    }

    private List<RecipeResponseDto> recommend() {
        em.flush();
        em.clear(); // 회원의 취향·알레르기 목록을 DB에서 다시 읽게 한다
        return service.recommendFromFridge(member.getId());
    }

    private static List<String> names(List<RecipeResponseDto> recipes) {
        return recipes.stream().map(RecipeResponseDto::getName).toList();
    }

    @Test
    void 임박_재료가_많이_겹치는_레시피가_전체_일치가_많은_레시피보다_앞이다() {
        fridge("두부", 1);
        fridge("애호박", 2);
        for (String n : List.of("감자", "당근", "양배추", "버섯")) fridge(n, 30);
        recipe("1", "두부애호박찌개", "두부", "애호박");                           // 임박 2, 전체 2
        recipe("2", "모둠채소두부볶음", "감자", "당근", "양배추", "버섯", "두부");   // 임박 1, 전체 5
        recipe("3", "감자당근볶음", "감자", "당근");                              // 임박 0, 전체 2

        List<RecipeResponseDto> result = recommend();

        assertThat(names(result)).containsExactly("두부애호박찌개", "모둠채소두부볶음", "감자당근볶음");
        assertThat(result.get(0).getImminentCount()).isEqualTo(2);
        assertThat(result.get(1).getMatchedCount()).isEqualTo(5);
    }

    @Test
    void 임박_재료가_없으면_겹치는_재료가_많은_순_같으면_부족한_재료가_적은_순_그다음_일련번호_순() {
        for (String n : List.of("감자", "당근", "양파")) fridge(n, 30);
        recipe("10", "감자조림", "감자", "물엿");                         // 일치 1, 부족 0(물엿은 양념)
        recipe("9", "감자국", "감자", "두부");                            // 일치 1, 부족 1
        recipe("8", "감자전", "감자", "부추");                            // 일치 1, 부족 1 → 일련번호 8이 9보다 앞
        recipe("7", "카레", "감자", "당근", "양파", "카레가루", "돼지고기");  // 일치 3, 부족 2

        assertThat(names(recommend())).containsExactly("카레", "감자조림", "감자전", "감자국");
    }

    @ParameterizedTest(name = "소비기한 {0}일 남음 → 임박 {1}, 지남 {2}")
    @CsvSource({"0, true, false", "3, true, false", "4, false, false", "-1, false, true"})
    void 임박은_오늘부터_3일_이내_지난_재료는_일치로_세되_임박이_아니고_따로_알려준다(int daysLeft, boolean imminent, boolean expired) {
        fridge("두부", daysLeft);
        recipe("1", "두부조림", "두부", "간장");

        RecipeResponseDto dto = recommend().get(0);

        assertThat(dto.getMatchedCount()).isEqualTo(1);
        assertThat(dto.getImminentCount()).isEqualTo(imminent ? 1 : 0);
        assertThat(dto.getExpiredIngredients()).isEqualTo(expired ? List.of("두부") : List.of());
    }

    @Test
    void 소비기한이_없는_재료는_임박이_아니다() {
        fridge("두부", null);
        recipe("1", "두부조림", "두부");

        assertThat(recommend().get(0).getImminentCount()).isZero();
    }

    @Test
    void 양념만_겹치는_레시피는_빠지고_양념은_부족한_재료에도_없다() {
        fridge("간장", 1);
        fridge("감자", 30);
        recipe("1", "간장소스", "간장", "설탕", "식초");
        recipe("2", "감자조림", "감자", "간장", "설탕", "당근");

        List<RecipeResponseDto> result = recommend();

        assertThat(names(result)).containsExactly("감자조림");
        assertThat(result.get(0).getMatchedCount()).isEqualTo(1);
        assertThat(result.get(0).getMissingIngredients()).containsExactly("당근");
    }

    @Test
    void 냉장고_재료의_동의어로_적힌_레시피도_일치로_센다() {
        fridge("계란", 30);
        recipe("1", "달걀찜", "달걀", "새우젓");

        assertThat(names(recommend())).containsExactly("달걀찜");
    }

    @Test
    void 알레르기_재료가_동의어로라도_원문에_있으면_빠진다() {
        Ingredient egg = new Ingredient();
        egg.setName("달걀");
        egg.setCategory(Category.EGG);
        em.persist(egg);
        em.persist(new Allergy(member, egg));
        fridge("두부", 30);
        recipe("1", "두부계란부침", "두부", "계란");
        recipe("2", "두부조림", "두부", "간장");

        assertThat(names(recommend())).containsExactly("두부조림");
    }

    @Test
    void 싫어요한_레시피만_빠지고_이름이_같은_다른_레시피는_남는다() {
        fridge("두부", 30);
        Recipe disliked = recipe("1", "두부조림", "두부");
        recipe("2", "두부조림", "두부");
        em.persist(new Taste("싫어요", disliked, member));

        List<RecipeResponseDto> result = recommend();

        assertThat(result).extracting(RecipeResponseDto::getId).doesNotContain(disliked.getId()).hasSize(1);
    }

    @Test
    void 결과는_15건까지() {
        fridge("두부", 30);
        for (int i = 1; i <= 20; i++) recipe(String.valueOf(i), "두부요리" + i, "두부");

        List<RecipeResponseDto> result = recommend();

        assertThat(result).hasSize(15);
        assertThat(names(result).get(0)).isEqualTo("두부요리1");
    }

    @Test
    void 냉장고가_비었거나_겹치는_레시피가_없으면_빈_목록() {
        recipe("1", "두부조림", "두부");
        assertThat(recommend()).isEmpty();

        fridge("우주식량", 30);
        assertThat(recommend()).isEmpty();
    }

    @Test
    void 레시피를_저장하지_않는다() {
        fridge("두부", 30);
        recipe("1", "두부조림", "두부");
        long before = recipeRepository.count();

        recommend();

        assertThat(recipeRepository.count()).isEqualTo(before);
    }

    @Test
    void 없는_회원이면_404() {
        assertThatThrownBy(() -> service.recommendFromFridge(999_999L)).isInstanceOf(EntityNotFoundException.class);
    }
}
