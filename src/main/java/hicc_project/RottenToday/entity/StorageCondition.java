package hicc_project.RottenToday.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StorageCondition {
    NORMAL(0, "실온"),
    REFRIGERATED(1, "냉장"),
    FROZEN(2, "냉동");

    private final int id;
    private final String type;
}
