package hicc_project.RottenToday.service;

import hicc_project.RottenToday.entity.*;
import hicc_project.RottenToday.repository.*;
import hicc_project.RottenToday.service.recipe.RecipeIngredientParser;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

// 목록 조회가 행마다 연관 엔티티를 따로 조회하는지(N+1) SQL 수로 확인한다 (성능 점검, P-014)
// 회원 1명에 기록·취향·알레르기·냉장고 재료 15건씩 만들고 실제 서비스 메서드를 불러 실행된 SQL 수를 센다
// 결과는 build/reports/query-count/result.txt에도 남긴다
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class QueryCountTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    static final int ROWS = 15;
    private static final Map<String, Long> RESULTS = new TreeMap<>();

    @Autowired private TestEntityManager em;
    @Autowired private EntityManagerFactory emf;
    @Autowired private TasteRepository tasteRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private HistoryRepository historyRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AllergyRepository allergyRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private RefrigeratorIngredientRepository fridgeRepository;
    @Autowired private RecipeIngredientRepository recipeIngredientRepository;

    private UserService userService;
    private IngredientService ingredientService;
    private RecipeRecommendService recommendService;
    private Member member;

    @BeforeEach
    void setUp() {
        RecipeIngredientParser parser = new RecipeIngredientParser(List.of());
        userService = new UserService(tasteRepository, recipeRepository, historyRepository, memberRepository, allergyRepository, ingredientRepository);
        ingredientService = new IngredientService(fridgeRepository, ingredientRepository, memberRepository, parser);
        Clock clock = Clock.fixed(LocalDate.of(2026, 10, 9).atTime(9, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
        recommendService = new RecipeRecommendService(ingredientService, recipeIngredientRepository, recipeRepository, memberRepository,
                parser, clock, 3, 15, List.of(RecipeRecommendService.DEFAULT_SEASONINGS.split(",")));

        member = em.persist(Member.builder().email("q@test").externalId("q").provider("google").tokenVersion(0L).build());
        // 기본 재료 추천용 마스터 (마늘은 냉장고에 있음)
        for (String name : List.of("간장", "소금", "된장", "고추장", "식용유", "마늘", "쌀")) ingredient(name);
        fridge("마늘", ingredientRepository.findByName("마늘").orElseThrow());

        for (int i = 1; i <= ROWS; i++) {
            Ingredient food = ingredient("재료" + i);
            Recipe recipe = recipe(String.valueOf(i), "레시피" + i, "재료" + i, "부재료" + i);
            fridge("재료" + i, food);
            em.persist(new History(member, LocalDateTime.of(2026, 10, 9, 9, 0).plusMinutes(i), recipe));
            em.persist(new Taste("좋아요", recipe, member));
            em.persist(new Allergy(member, ingredient("알레르기" + i)));
        }
        em.flush();
    }

    private Ingredient ingredient(String name) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setCategory(Category.ETC);
        return em.persist(ingredient);
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

    private void fridge(String name, Ingredient master) {
        RefrigeratorIngredient item = new RefrigeratorIngredient();
        item.setName(name);
        item.setQuantity(1);
        item.setUnit("개");
        item.setType(StorageCondition.REFRIGERATED);
        item.setCategory(Category.ETC);
        item.setMember(member);
        item.setIngredient(master);
        item.setInput_date(LocalDateTime.of(2026, 10, 9, 0, 0));
        item.setExpire_date(LocalDateTime.of(2026, 10, 20, 0, 0));
        em.persist(item);
    }

    /** 영속성 컨텍스트를 비운 뒤 한 번 호출하고 실행된 SQL 수를 센다 */
    private long sqlCount(String label, Supplier<?> call) {
        em.flush();
        em.clear();
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        call.get();
        long count = stats.getPrepareStatementCount();
        RESULTS.put(label, count);
        System.out.printf("query-count %s sql=%d%n", label, count);
        return count;
    }

    // 행이 15건이어도 SQL 수가 행 수만큼 늘지 않아야 한다. 수정 전(2026-10-09)은 목록마다 17~18번, 기본 재료 24번, 추천 24번이었다
    @Test
    void 목록_조회의_SQL_수가_행_수만큼_늘지_않는다() {
        Long id = member.getId();
        assertThat(sqlCount("기록 목록 (기록 15건)", () -> userService.getHistory(id))).isLessThanOrEqualTo(4);
        assertThat(sqlCount("즐겨찾기 목록 (기록 15건)", () -> userService.getFavorites(id))).isLessThanOrEqualTo(4);
        assertThat(sqlCount("좋아요·싫어요 목록 (15건)", () -> userService.getTaste(id))).isLessThanOrEqualTo(4);
        assertThat(sqlCount("알레르기 목록 (15건)", () -> userService.getAllergy(id))).isLessThanOrEqualTo(4);
        assertThat(sqlCount("냉장고 목록 (16건)", () -> ingredientService.getRefidge(id))).isLessThanOrEqualTo(4);
        assertThat(sqlCount("기본 재료 추천 (빠진 6개)", () -> ingredientService.getNecessaryIngredients(id))).isLessThanOrEqualTo(5);
        assertThat(sqlCount("레시피 추천 (후보 15개)", () -> recommendService.recommendFromFridge(id))).isLessThanOrEqualTo(10);
    }

    @Test
    void 기본_재료_추천은_한_번에_조회해도_순서와_결과가_같다() {
        List<String> names = ingredientService.getNecessaryIngredients(member.getId()).getIngredientList().stream()
                .map(r -> r.getName()).toList();

        assertThat(names).containsExactly("간장", "소금", "된장", "고추장", "식용유", "쌀"); // 마늘은 냉장고에 있음
    }

    @AfterAll
    static void writeReport() throws IOException {
        Path out = Path.of("build/reports/query-count/result.txt");
        Files.createDirectories(out.getParent());
        StringBuilder sb = new StringBuilder();
        RESULTS.forEach((k, v) -> sb.append(k).append(" sql=").append(v).append('\n'));
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
    }
}
