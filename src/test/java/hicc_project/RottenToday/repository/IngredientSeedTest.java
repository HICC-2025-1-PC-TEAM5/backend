package hicc_project.RottenToday.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

// B27: 재료 마스터 seed(scripts/seed/ingredient_master.sql)는 기준 데이터다. 이미 있는 행도 카테고리·영양성분·출처를 seed 값으로 맞추고,
// id와 이미지는 그대로 둔다. 여러 번 실행해도 결과가 같다
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class IngredientSeedTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;

    private void runSeed() throws Exception {
        // 테스트 트랜잭션의 연결로 실행해 테스트가 끝나면 함께 롤백되게 한다 (임시 테이블 생성은 MySQL에서 커밋을 일으키지 않는다)
        Connection connection = DataSourceUtils.getConnection(dataSource);
        ScriptUtils.executeSqlScript(connection,
                new EncodedResource(new FileSystemResource("scripts/seed/ingredient_master.sql"), "UTF-8"));
    }

    private int count() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ingredient", Integer.class);
    }

    @Test
    void 예전_seed로_들어간_행도_seed_값으로_맞추고_id와_이미지는_유지한다() throws Exception {
        // 예전 seed처럼 다른 대표 행(젓갈)·다른 카테고리로 들어간 행
        jdbcTemplate.update("INSERT INTO ingredient (name, category, energy_kcal, source_food_code, image_url) "
                + "VALUES ('꼴뚜기', 9, 92, 'R211-663014071-0000', 'https://img/kkol.png')");
        Long id = jdbcTemplate.queryForObject("SELECT id FROM ingredient WHERE name = '꼴뚜기'", Long.class);

        runSeed();

        assertThat(jdbcTemplate.queryForObject("SELECT id FROM ingredient WHERE name = '꼴뚜기'", Long.class)).isEqualTo(id);
        assertThat(jdbcTemplate.queryForObject("SELECT category FROM ingredient WHERE name = '꼴뚜기'", Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("SELECT source_food_code FROM ingredient WHERE name = '꼴뚜기'", String.class))
                .isEqualTo("R211-663074001-1197");
        assertThat(jdbcTemplate.queryForObject("SELECT image_url FROM ingredient WHERE name = '꼴뚜기'", String.class))
                .isEqualTo("https://img/kkol.png");
    }

    @Test
    void 영양성분은_알_껍질이_아니라_먹는_부위_행에서_가져온다() throws Exception {
        runSeed();

        // D-035: 예전 규칙은 '대표'가 붙은 연어알(250kcal)·새우 껍질(323kcal) 행을 골랐다
        assertThat(jdbcTemplate.queryForObject("SELECT source_food_code FROM ingredient WHERE name = '연어'", String.class))
                .isEqualTo("R211-201034001-0000");
        assertThat(jdbcTemplate.queryForObject("SELECT energy_kcal FROM ingredient WHERE name = '연어'", Double.class)).isEqualTo(120.0);
        // 새우는 원본에 생 살코기 행이 없어 흰다리새우 값을 쓴다. 카테고리는 어패류 그대로
        assertThat(jdbcTemplate.queryForObject("SELECT source_food_code FROM ingredient WHERE name = '새우'", String.class))
                .isEqualTo("R211-717414001-0000");
        assertThat(jdbcTemplate.queryForObject("SELECT category FROM ingredient WHERE name = '새우'", Integer.class)).isEqualTo(4);
    }

    @Test
    void 두_번_실행해도_결과가_같다() throws Exception {
        runSeed();
        int first = count();
        String squid = jdbcTemplate.queryForObject("SELECT CONCAT(category, '/', source_food_code) FROM ingredient WHERE name = '오징어'", String.class);

        runSeed();

        assertThat(first).isEqualTo(1072);
        assertThat(count()).isEqualTo(first);
        assertThat(jdbcTemplate.queryForObject("SELECT CONCAT(category, '/', source_food_code) FROM ingredient WHERE name = '오징어'", String.class))
                .isEqualTo(squid);
    }
}
