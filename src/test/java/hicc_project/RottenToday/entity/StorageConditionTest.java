package hicc_project.RottenToday.entity;

import hicc_project.RottenToday.dto.RefridgeDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// C3: 보관방식 문자열은 BE enum 값(실온/냉장실/냉동고)만 받는다 (D-015)
class StorageConditionTest {

    @ParameterizedTest
    @CsvSource({"실온, NORMAL", "냉장실, REFRIGERATED", "냉동고, FROZEN"})
    void 기준값은_enum으로_바뀐다(String type, StorageCondition expected) {
        assertThat(StorageCondition.fromType(type)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"냉장고", "냉동실", "냉장", "fridge", "FROZEN", " 실온 "})
    void 기준값이_아니면_IllegalArgumentException(String type) {
        assertThatThrownBy(() -> StorageCondition.fromType(type))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("보관 방식은 '실온', '냉장실', '냉동고' 중 하나여야 합니다.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void 빈_값이면_IllegalArgumentException(String type) {
        assertThatThrownBy(() -> StorageCondition.fromType(type))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 냉동고로_등록하면_냉동으로_저장된다() { // 수정 전에는 냉장(REFRIGERATED)으로 저장됐다
        RefridgeDto dto = new RefridgeDto();
        dto.setName("만두");
        dto.setQuantity(1);
        dto.setType("냉동고");

        assertThat(new RefrigeratorIngredient(dto).getType()).isEqualTo(StorageCondition.FROZEN);
    }
}
