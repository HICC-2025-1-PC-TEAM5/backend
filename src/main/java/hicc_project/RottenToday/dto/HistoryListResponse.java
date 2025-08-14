package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.History;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HistoryListResponse {
    List<HistoryDto> history;

    public HistoryListResponse(List<HistoryDto> history) {
        this.history = history;
    }




}
