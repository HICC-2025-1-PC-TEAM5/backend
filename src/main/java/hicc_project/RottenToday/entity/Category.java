package hicc_project.RottenToday.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Category {
    FRUIT(0, "과일"),
    GRAIN(1, "곡물"),
    VEGETABLE(2, "채소"),
    MEAT(3, "육류"),
    SEAFOOD(4, "수산물"),
    CONDIMENT(5, "조미료"),
    DAIRY(6, "유제품"),
    ETC(7, "기타");

    private final int id;
    private final String type;
}
