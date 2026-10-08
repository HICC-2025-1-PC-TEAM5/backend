package hicc_project.RottenToday;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// 앱 전체가 뜨는지 확인한다(빈 주입·설정·엔티티와 Flyway 스키마 일치). DB는 테스트용 MySQL 컨테이너를 쓴다 (D-023)
// git에 없는 application.properties(비밀값) 없이도 뜨도록 필수 키에 가짜 값을 넣는다. 외부 API는 호출하지 않는다
// Docker가 없는 환경에서는 건너뛴다
@SpringBootTest(properties = {
		"cloud.aws.credentials.access-key=test",
		"cloud.aws.credentials.secret-key=test",
		"cloud.aws.region.static=ap-northeast-2",
		"cloud.aws.s3.bucket=test",
		"clova.ocr.secret=test",
		"clova.ocr.url=http://localhost/test",
		"spring.security.oauth2.client.registration.google.client-id=test",
		"spring.security.oauth2.client.registration.google.client-secret=test",
		"app.jwt.secret=test-secret-for-context-loads-0123456789-abcdef",
})
@Testcontainers(disabledWithoutDocker = true)
class RottenTodayApplicationTests {

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

	@Test
	void contextLoads() {
	}

}
