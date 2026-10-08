package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.RecipeResponseDto;
import hicc_project.RottenToday.dto.RecipeStepDto;
import hicc_project.RottenToday.dto.SubstituteDto;
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
        RecipeIngredientParser parser = new RecipeIngredientParser(
                List.of(new RecipeIngredientParser.AliasEntry("계란", "달걀", true)),
                List.of(new RecipeIngredientParser.SubstituteEntry("닭고기살", "닭고기"),
                        new RecipeIngredientParser.SubstituteEntry("통깨", "참깨"),
                        new RecipeIngredientParser.SubstituteEntry("후춧가루", "후추")));
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
    void 일련번호는_숫자로_비교한다() {
        fridge("두부", 30);
        recipe("10", "두부요리10", "두부");
        recipe("9", "두부요리9", "두부");
        recipe("100", "두부요리100", "두부");

        assertThat(names(recommend())).containsExactly("두부요리9", "두부요리10", "두부요리100");
    }

    @Test
    void 같은_재료가_지난_것과_새것으로_있으면_일치로_세고_지난_재료로도_알려준다() {
        fridge("두부", -2);
        fridge("두부", 2);
        recipe("1", "두부조림", "두부");

        RecipeResponseDto dto = recommend().get(0);

        assertThat(dto.getImminentCount()).isEqualTo(1);
        assertThat(dto.getExpiredIngredients()).containsExactly("두부");
    }

    @Test
    void 냉장고에_양념만_있으면_빈_목록() {
        fridge("간장", 1);
        fridge("소금", 30);
        recipe("1", "간장소스", "간장", "소금", "양파");

        assertThat(recommend()).isEmpty();
    }

    @Test
    void 재료가_없는_알레르기_행이_있어도_실패하지_않는다() {
        em.persist(new Allergy(member, null));
        fridge("두부", 30);
        recipe("1", "두부조림", "두부");

        assertThat(names(recommend())).containsExactly("두부조림");
    }

    @Test
    void 응답의_조리_단계는_값으로_채워진다() {
        fridge("두부", 30);
        Recipe recipe = recipe("1", "두부조림", "두부");
        RecipeStep step = new RecipeStep(1, "두부를 썬다.", null);
        step.setRecipe(recipe);
        recipe.setRecipeSteps(new ArrayList<>(List.of(step)));
        em.persist(step);

        RecipeResponseDto dto = recommend().get(0);

        // 엔티티가 아니라 값으로 복사한 DTO여야 트랜잭션 밖 직렬화에서 실패하지 않는다
        assertThat(dto.getSteps()).hasOnlyElementsOfType(RecipeStepDto.class);
        assertThat(dto.getSteps()).extracting(RecipeStepDto::description).containsExactly("두부를 썬다.");
    }

    @Test
    void 없는_회원이면_404() {
        assertThatThrownBy(() -> service.recommendFromFridge(999_999L)).isInstanceOf(EntityNotFoundException.class);
    }

    // D-038: 레시피 상세의 지난 재료는 추천과 같은 규칙(정규화·동의어, 양념 제외)으로 레시피 재료만 본다
    @Test
    void 상세의_지난_재료는_레시피_재료_중_기한이_지난_것만_동의어까지_맞춰_준다() {
        fridge("계란", -1);   // 동의어 → 달걀
        fridge("두부", -2);
        fridge("애호박", 5);   // 아직 괜찮음
        fridge("간장", -1);   // 양념은 안내하지 않음
        fridge("우유", -3);   // 레시피에 없는 재료(대체 재료 후보)는 안내하지 않음
        Recipe recipe = recipe("1", "두부달걀찜", "달걀", "두부", "애호박", "간장");
        em.flush();
        em.clear();

        assertThat(service.detailNotesOf(member.getId(), recipe.getId()).expiredIngredients())
                .containsExactlyInAnyOrder("달걀", "두부");
    }

    // D-040: 대체 가능 재료
    @Test
    void 냉장고에_없어도_대신_쓸_재료가_있으면_부족이_아니라_대체_가능으로_알린다() {
        fridge("닭고기", 30);
        fridge("대파", 30);
        recipe("1", "닭볶음", "닭고기살", "대파", "감자");

        RecipeResponseDto dto = recommend().get(0);

        assertThat(dto.getMatchedCount()).isEqualTo(1); // 대체 재료는 일치로 세지 않는다
        assertThat(dto.getMissingIngredients()).containsExactly("감자");
        assertThat(dto.getSubstitutes()).containsExactly(new SubstituteDto("닭고기살", "닭고기"));
    }

    @Test
    void 대체_가능한_재료는_부족_수에서_빠져_일치_수가_같으면_앞선다() {
        fridge("대파", 30);
        fridge("닭고기", 30);
        recipe("1", "대파감자당근볶음", "대파", "감자", "당근");    // 부족 2
        recipe("2", "닭살대파감자볶음", "대파", "감자", "닭고기살"); // 부족 1 + 대체 1

        assertThat(names(recommend())).containsExactly("닭살대파감자볶음", "대파감자당근볶음");
    }

    @Test
    void 양념을_대신하는_재료도_양념으로_보아_부족에_넣지_않는다() {
        fridge("두부", 30);
        recipe("1", "두부구이", "두부", "후춧가루", "통깨");

        RecipeResponseDto dto = recommend().get(0);

        assertThat(dto.getMissingIngredients()).isEmpty();
        assertThat(dto.getSubstitutes()).isEmpty();
    }

    @Test
    void 알레르기는_대체_가능한_재료_이름도_원문에서_거른다() {
        Ingredient sesame = new Ingredient();
        sesame.setName("참깨");
        sesame.setCategory(Category.ETC);
        em.persist(sesame);
        em.persist(new Allergy(member, sesame));
        fridge("두부", 30);
        recipe("1", "두부통깨무침", "두부", "통깨");
        recipe("2", "두부조림", "두부", "간장");

        assertThat(names(recommend())).containsExactly("두부조림");
    }

    @Test
    void 상세에도_대체_가능_재료를_준다() {
        fridge("닭고기", 30);
        Recipe recipe = recipe("1", "닭죽", "닭고기살", "쌀");
        em.flush();
        em.clear();

        RecipeRecommendService.DetailNotes notes = service.detailNotesOf(member.getId(), recipe.getId());

        assertThat(notes.substitutes()).containsExactly(new SubstituteDto("닭고기살", "닭고기"));
        assertThat(notes.expiredIngredients()).isEmpty();
    }

    @Test
    void 상세의_지난_재료가_없으면_빈_목록() {
        fridge("두부", 2);
        Recipe recipe = recipe("1", "두부조림", "두부");
        em.flush();
        em.clear();

        assertThat(service.detailNotesOf(member.getId(), recipe.getId()).expiredIngredients()).isEmpty();
    }
}
