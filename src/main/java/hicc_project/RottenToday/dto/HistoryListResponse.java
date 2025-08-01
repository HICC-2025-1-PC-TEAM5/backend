package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.History;

import java.util.List;

public class HistoryListResponse {
    List<History> recipe;

    public HistoryListResponse(List<History> recipe) {
        this.recipe = recipe;
    }

    public List<History> getRecipe() {
        return recipe;
    }


}
