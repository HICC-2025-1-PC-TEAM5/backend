package hicc_project.RottenToday.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// R13: 좋아요/싫어요 문자열은 Appetite enum으로 바꿔 비교한다. 그 외·빈 값은 400(IllegalArgumentException)
class AppetiteTest {

    @ParameterizedTest
    @CsvSource({"좋아요, LIKE", "싫어요, DISLIKE"})
    void 기준값은_enum으로_바뀐다(String status, Appetite expected) {
        assertThat(Appetite.fromStatus(status)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"좋음", "LIKE", " 좋아요", "보통"})
    void 기준값이_아니면_IllegalArgumentException(String status) {
        assertThatThrownBy(() -> Appetite.fromStatus(status))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("type 변수값으로 '좋아요' 혹은 '싫어요'만 입력할 수 있습니다.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void 빈_값이면_IllegalArgumentException(String status) { // 전에는 null이면 NullPointerException → 500
        assertThatThrownBy(() -> Appetite.fromStatus(status)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 취향은_문자열로_만들어도_enum으로_저장된다() {
        assertThat(new Taste("좋아요", null, null).getType()).isEqualTo(Appetite.LIKE);
        assertThat(new Taste("싫어요", null, null).getType()).isEqualTo(Appetite.DISLIKE);
        // 전에는 좋아요가 아니면 모두 싫어요로 저장됐다
        assertThatThrownBy(() -> new Taste("보통", null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
