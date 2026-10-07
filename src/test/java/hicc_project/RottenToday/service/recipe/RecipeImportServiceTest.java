package hicc_project.RottenToday.service.recipe;

import hicc_project.RottenToday.dto.RecipeDto;
import hicc_project.RottenToday.entity.Category;
import hicc_project.RottenToday.entity.Ingredient;
import hicc_project.RottenToday.repository.IngredientRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 레시피 계획 Phase 4: CSV 레시피를 RCP_SEQ 기준으로 upsert한다. 실제 MySQL에서 행 수로 멱등성을 확인한다 (D-020, D-023)
// 묶음마다 실제로 커밋되는지 보려고 테스트 트랜잭션을 쓰지 않고, 끝나면 직접 지운다
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker = true)
class RecipeImportServiceTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired private RecipeRepository recipeRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private RecipeImportService importService;

    @BeforeEach
    void setUp() {
        Ingredient tofu = new Ingredient();
        tofu.setName("두부");
        tofu.setCategory(Category.BEANS);
        Ingredient egg = new Ingredient();
        egg.setName("달걀");
        egg.setCategory(Category.EGG);
        ingredientRepository.saveAll(List.of(tofu, egg));

        RecipeIngredientParser parser = new RecipeIngredientParser(List.of(
                new RecipeIngredientParser.AliasEntry("계란", "달걀", true)));
        importService = new RecipeImportService(recipeRepository, ingredientRepository, parser,
                new TransactionTemplate(transactionManager), 2);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM recipe_ingredient");
        jdbcTemplate.update("DELETE FROM recipe_step");
        jdbcTemplate.update("DELETE FROM recipe");
        jdbcTemplate.update("DELETE FROM ingredient");
    }

    private static RecipeDto recipe(String seq, String name, String parts, String... steps) {
        RecipeDto dto = new RecipeDto();
        dto.setRCP_SEQ(seq);
        dto.setRCP_NM(name);
        dto.setRCP_PARTS_DTLS(parts);
        dto.setRCP_PAT2("반찬");
        dto.setINFO_ENG(100.0);
        if (steps.length > 0) dto.setMANUAL01(steps[0]);
        if (steps.length > 1) dto.setMANUAL02(steps[1]);
        if (steps.length > 2) dto.setMANUAL03(steps[2]);
        return dto;
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private List<RecipeDto> sample() {
        return List.of(
                recipe("1", "두부조림", "두부 1모, 간장 2큰술", "1. 두부를 썬다.", "2. 졸인다."),
                recipe("2", "계란찜", "계란 2개, 새우젓 약간", "1. 계란을 푼다."),
                recipe("3", "두부계란부침", "두부 1/2모, 계란 1개", "1. 두부에 계란물을 입힌다.", "2. 부친다."));
    }

    @Test
    void 레시피_단계_재료를_저장하고_마스터에_연결한다() {
        RecipeImportService.Summary summary = importService.importRecipes(sample());

        assertThat(summary.created()).isEqualTo(3);
        assertThat(summary.updated()).isZero();
        assertThat(summary.failed()).isEmpty();
        assertThat(count("recipe")).isEqualTo(3);
        assertThat(count("recipe_step")).isEqualTo(5);
        assertThat(count("recipe_ingredient")).isEqualTo(6);
        // 두부·달걀(계란)은 연결, 간장·새우젓은 마스터에 없어 확인 표시
        assertThat(summary.linkedCount()).isEqualTo(4);
        assertThat(jdbcTemplate.queryForList(
                "SELECT name FROM recipe_ingredient WHERE needs_review = 1 ORDER BY name", String.class))
                .containsExactly("간장", "새우젓");
        assertThat(jdbcTemplate.queryForList(
                "SELECT DISTINCT name FROM recipe_ingredient WHERE ingredient_id IS NOT NULL ORDER BY name", String.class))
                .containsExactly("달걀", "두부");
        assertThat(summary.topUnlinked()).extracting(Map.Entry::getKey).containsExactly("간장", "새우젓");
    }

    @Test
    void 같은_데이터로_두_번_적재해도_행_수가_같다() {
        importService.importRecipes(sample());
        int recipes = count("recipe"), steps = count("recipe_step"), ingredients = count("recipe_ingredient");

        RecipeImportService.Summary second = importService.importRecipes(sample());

        assertThat(second.created()).isZero();
        assertThat(second.updated()).isEqualTo(3);
        assertThat(count("recipe")).isEqualTo(recipes);
        assertThat(count("recipe_step")).isEqualTo(steps);
        assertThat(count("recipe_ingredient")).isEqualTo(ingredients);
    }

    @Test
    void 바뀐_레시피는_단계와_재료를_교체한다() {
        importService.importRecipes(sample());
        Long id = recipeRepository.findByRcpSeq("1").orElseThrow().getId();

        importService.importRecipes(List.of(
                recipe("1", "두부조림", "두부 1모", "1. 두부를 썬다.", "2. 굽는다.", "3. 졸인다.")));

        assertThat(recipeRepository.findByRcpSeq("1").orElseThrow().getId()).isEqualTo(id); // 기록이 참조하는 id 유지
        assertThat(jdbcTemplate.queryForList("SELECT description FROM recipe_step WHERE recipe_id = ? ORDER BY step_num",
                String.class, id)).containsExactly("1. 두부를 썬다.", "2. 굽는다.", "3. 졸인다.");
        assertThat(jdbcTemplate.queryForList("SELECT name FROM recipe_ingredient WHERE recipe_id = ?", String.class, id))
                .containsExactly("두부");
    }

    @Test
    void 실패한_묶음만_롤백하고_나머지는_저장한다() {
        // 묶음 크기 2: [1, 2] / [3, 실패] / [5]. 이름이 255자를 넘으면 저장에 실패한다
        List<RecipeDto> recipes = List.of(
                recipe("1", "두부조림", "두부 1모"),
                recipe("2", "계란찜", "계란 2개"),
                recipe("3", "두부부침", "두부 1모"),
                recipe("4", "가".repeat(300), "두부 1모"),
                recipe("5", "달걀말이", "달걀 3개"));

        RecipeImportService.Summary summary = importService.importRecipes(recipes);

        assertThat(summary.failed()).containsExactly("3", "4");
        assertThat(summary.created()).isEqualTo(3);
        assertThat(jdbcTemplate.queryForList("SELECT rcp_seq FROM recipe ORDER BY rcp_seq", String.class))
                .containsExactly("1", "2", "5");

        // 고친 데이터로 다시 실행하면 빠진 레시피만 추가된다
        RecipeImportService.Summary retry = importService.importRecipes(List.of(
                recipe("1", "두부조림", "두부 1모"), recipe("2", "계란찜", "계란 2개"),
                recipe("3", "두부부침", "두부 1모"), recipe("4", "두부튀김", "두부 1모"), recipe("5", "달걀말이", "달걀 3개")));
        assertThat(retry.failed()).isEmpty();
        assertThat(count("recipe")).isEqualTo(5);
    }

    @Test
    void 재료_문자열이_없는_레시피도_저장한다() {
        RecipeImportService.Summary summary = importService.importRecipes(List.of(recipe("1", "물", null)));

        assertThat(summary.failed()).isEmpty();
        assertThat(count("recipe")).isEqualTo(1);
        assertThat(count("recipe_ingredient")).isZero();
    }
}
