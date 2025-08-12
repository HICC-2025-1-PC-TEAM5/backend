package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.History;

import java.util.List;

public class HistoryListResponse {
    List<HistoryDto> history;

    public HistoryListResponse(List<HistoryDto> recipe) {
        this.history = recipe;
    }

    public List<HistoryDto> getRecipe() {
        return history;
    }


}
