package hicc_project.RottenToday.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Appetite {
    DISLIKE(0, "싫어요"),
    LIKE(1, "좋아요");

    private int id;
    private String status;

}
