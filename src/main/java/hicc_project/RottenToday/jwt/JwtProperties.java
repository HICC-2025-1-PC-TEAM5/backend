package hicc_project.RottenToday.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;                // HS256 secret
    private long accessTtlSeconds  = 3600;      // 1h
    private long refreshTtlSeconds = 1209600;   // 14d
}
