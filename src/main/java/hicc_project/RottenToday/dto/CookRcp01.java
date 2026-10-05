package hicc_project.RottenToday.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CookRcp01 {
    private int total_count;
    private List<RecipeDto> row;

    // 결과가 없거나 오류일 때 row 대신 온다 (INFO-200: 데이터 없음, 그 외: 인증키 오류 등)
    @JsonProperty("RESULT")
    private Result result;

    @Getter
    @Setter
    public static class Result {
        @JsonProperty("CODE")
        private String code;
        @JsonProperty("MSG")
        private String msg;
    }
}
