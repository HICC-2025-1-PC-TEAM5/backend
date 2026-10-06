package hicc_project.RottenToday.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StorageCondition {
    NORMAL(0, "실온"),
    REFRIGERATED(1, "냉장실"),
    FROZEN(2, "냉동고");

    private final int id;
    private final String type;

    // API로 받는 보관방식 문자열은 type 값(실온/냉장실/냉동고)만 허용한다 (D-015)
    public static StorageCondition fromType(String type) {
        for (StorageCondition condition : values()) {
            if (condition.type.equals(type)) {
                return condition;
            }
        }
        throw new IllegalArgumentException("보관 방식은 '실온', '냉장실', '냉동고' 중 하나여야 합니다.");
    }
}
