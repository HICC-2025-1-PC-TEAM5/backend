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

    // API로 받는 취향 문자열은 status 값(좋아요/싫어요)만 허용한다. null·그 외는 400 (StorageCondition.fromType과 같은 방식)
    public static Appetite fromStatus(String status) {
        for (Appetite appetite : values()) {
            if (appetite.status.equals(status)) {
                return appetite;
            }
        }
        throw new IllegalArgumentException("type 변수값으로 '좋아요' 혹은 '싫어요'만 입력할 수 있습니다.");
    }
}
