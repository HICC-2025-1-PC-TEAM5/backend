package hicc_project.RottenToday.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Category {
    VEGETABLE(0, "채소류"),
    FRUIT(1, "과일류"),
    GRAIN(2, "곡류/전분류"),
    MEAT(3, "육류"),
    SEAFOOD(4, "어패류"),
    EGG(5, "달걀/난류"),
    DAIRY(6, "유제품"),
    BEANS(7, "두류/콩류"),
    OIL(8, "기름/지방류"),
    CONDIMENT(9, "조미료/향신료"),
    PROCESSED(10, "가공식품"),
    DRINK(11, "음료류"),
    ETC(12, "기타");

    private final int id;
    private final String type;
}
