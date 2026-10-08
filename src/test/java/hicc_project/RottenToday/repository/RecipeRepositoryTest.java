package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.RecipeIngredient;
import hicc_project.RottenToday.entity.RecipeStep;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 레시피 계획 Phase 2: Flyway 마이그레이션(V1 기준 스키마 + V2)을 실제 MySQL에 적용하고, 엔티티와 스키마가 맞는지(validate) 확인한다 (D-023)
// Docker가 없는 환경에서는 건너뛴다
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class RecipeRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    private static Recipe recipe(String rcpSeq, String name) {
        Recipe recipe = new Recipe();
        recipe.setRcpSeq(rcpSeq);
        recipe.setName(name);
        return recipe;
    }

    @Test
    void 마이그레이션이_V3까지_적용된다() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank", String.class);

        assertThat(versions).containsExactly("1", "2", "3");
    }

    @Test
    void 같은_레시피_일련번호는_두_번_저장할_수_없다() {
        recipeRepository.saveAndFlush(recipe("28", "새우 두부 계란찜"));

        assertThatThrownBy(() -> recipeRepository.saveAndFlush(recipe("28", "새우 두부 계란찜")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 일련번호가_없는_기존_레시피는_여러_개_저장된다() {
        recipeRepository.saveAndFlush(recipe(null, "요청마다 저장한 레시피"));
        recipeRepository.saveAndFlush(recipe(null, "요청마다 저장한 레시피"));

        assertThat(recipeRepository.count()).isEqualTo(2);
    }

    @Test
    void 일련번호로_찾는다() {
        recipeRepository.saveAndFlush(recipe("28", "새우 두부 계란찜"));
        recipeRepository.saveAndFlush(recipe("29", "부추 콩가루 찜"));
        recipeRepository.saveAndFlush(recipe("30", "방울토마토 소박이"));

        assertThat(recipeRepository.findByRcpSeq("29")).get().extracting(Recipe::getName).isEqualTo("부추 콩가루 찜");
        assertThat(recipeRepository.findAllByRcpSeqIn(List.of("28", "30", "999")))
                .extracting(Recipe::getRcpSeq).containsExactlyInAnyOrder("28", "30");
    }

    @Test
    void 재료_원문과_단계_설명은_255자를_넘어도_저장된다() {
        Recipe recipe = recipe("31", "긴 레시피");
        recipe.setIngredients("가".repeat(3000));
        RecipeStep step = new RecipeStep(1, "나".repeat(1000), null);
        step.setRecipe(recipe);
        recipe.setRecipeSteps(List.of(step));

        Recipe saved = recipeRepository.saveAndFlush(recipe);

        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM recipe_step WHERE recipe_id = ?", String.class, saved.getId());
        assertThat(description).hasSize(1000);
    }

    @Test
    void 레시피_재료에_정규화_이름과_확인_필요_표시를_저장한다() {
        Recipe saved = recipeRepository.saveAndFlush(recipe("32", "두부조림"));
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setRecipe(saved);
        ingredient.setName("두부");
        ingredient.setRawText("두부 1/2모");
        ingredient.setNeedsReview(true);
        entityManager.persistAndFlush(ingredient);
        entityManager.clear();

        RecipeIngredient found = entityManager.find(RecipeIngredient.class, ingredient.getId());
        assertThat(found.getName()).isEqualTo("두부");
        assertThat(found.getRawText()).isEqualTo("두부 1/2모");
        assertThat(found.isNeedsReview()).isTrue();
        // 추천 매칭에 쓰는 이름 인덱스
        assertThat(jdbcTemplate.queryForList(
                "SELECT index_name FROM information_schema.statistics WHERE table_schema = DATABASE() "
                        + "AND table_name = 'recipe_ingredient' AND column_name = 'name'", String.class))
                .contains("idx_recipe_ingredient_name");
    }

    @Test
    void V3는_분말_행으로_들어간_파프리카만_채소로_바로잡는다() throws Exception {
        // 예전 seed로 만든 DB처럼 분말 행을 넣고, V3 스크립트를 다시 실행한다 (컨테이너 DB에는 seed가 없어 V3가 아무것도 바꾸지 않았다)
        jdbcTemplate.update("INSERT INTO ingredient (name, category, energy_kcal, source_food_code) VALUES ('파프리카', 9, 520, 'R118-039100006-0000')");
        jdbcTemplate.update("INSERT INTO ingredient (name, category, energy_kcal, source_food_code) VALUES ('파프리카가루', 9, 520, 'R118-039100006-0000')");
        String v3 = new String(getClass().getResourceAsStream("/db/migration/V3__fix_paprika_master.sql").readAllBytes(), StandardCharsets.UTF_8);
        jdbcTemplate.execute(v3.replaceAll("(?m)^--.*$", "").trim().replaceAll(";$", ""));

        assertThat(jdbcTemplate.queryForObject("SELECT category FROM ingredient WHERE name = '파프리카'", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT energy_kcal FROM ingredient WHERE name = '파프리카'", Double.class)).isEqualTo(26.0);
        assertThat(jdbcTemplate.queryForObject("SELECT source_food_code FROM ingredient WHERE name = '파프리카'", String.class))
                .isEqualTo("R106-194008101-0000");
        // 이름이 다른 행(분말 재료)은 건드리지 않는다
        assertThat(jdbcTemplate.queryForObject("SELECT category FROM ingredient WHERE name = '파프리카가루'", Integer.class)).isEqualTo(9);
    }
}
