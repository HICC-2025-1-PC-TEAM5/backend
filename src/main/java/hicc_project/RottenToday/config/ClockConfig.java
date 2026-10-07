package hicc_project.RottenToday.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// 오늘 날짜에 따라 결과가 달라지는 로직(추천의 소비기한 임박 판단)을 테스트에서 고정 시각으로 검증하려고 둔다
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
