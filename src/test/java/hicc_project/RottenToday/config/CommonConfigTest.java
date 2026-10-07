package hicc_project.RottenToday.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

// D-032: 비밀값이 아닌 공통 설정은 git에 있는 classpath:/config/application.properties에서 읽힌다
// (로컬 비밀값 파일이 없는 새 클론에서도 Flyway·validate 설정이 적용되어야 한다)
class CommonConfigTest {

    @Configuration
    static class Empty {}

    @Test
    void 공통_설정이_적용된다() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Empty.class)
                .web(WebApplicationType.NONE).run()) {
            Environment env = context.getEnvironment();

            assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
            assertThat(env.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo("true");
            assertThat(env.getProperty("spring.flyway.baseline-version")).isEqualTo("1");
        }
    }
}
