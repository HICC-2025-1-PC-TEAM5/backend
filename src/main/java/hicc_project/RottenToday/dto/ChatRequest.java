package hicc_project.RottenToday.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {
    private String model;
    private List<ChatMessage> messages;
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    private Double temperature;
}
