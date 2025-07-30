package hicc_project.RottenToday.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Appetite {
    DISLIKE(0, "싫어하는 음식"),
    LIKE(1, "좋아하는 음식");

    private int id;
    private String status;

}
