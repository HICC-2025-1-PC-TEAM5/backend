package hicc_project.RottenToday.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Choice {
    private Integer index;
    private ChatMessage message;
    @JsonProperty("finish_reason")
    private String finishReason;
}
