package hicc_project.RottenToday.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {
    private String url;   // Chat Completions 엔드포인트
    private String key;   // API 키
}
