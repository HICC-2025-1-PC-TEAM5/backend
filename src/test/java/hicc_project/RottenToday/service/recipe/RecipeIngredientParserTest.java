package hicc_project.RottenToday.service.recipe;

import hicc_project.RottenToday.service.recipe.RecipeIngredientParser.ParsedIngredient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// 레시피 계획 Phase 1 (D-021): 샘플은 COOKRCP01.csv의 실제 RCP_PARTS_DTLS
class RecipeIngredientParserTest {

    // 규칙만 검증하려고 동의어 사전 없이 만든다
    private final RecipeIngredientParser parser = new RecipeIngredientParser(Map.of());

    private List<String> names(String parts) {
        return parser.parse(parts).stream().map(ParsedIngredient::name).toList();
    }

    static Stream<Arguments> samples() {
        return Stream.of(
                // 첫 줄 요리명, 머리말만 있는 줄("고명"), 괄호 안 분수, "1큰술"
                Arguments.of("새우두부계란찜\n연두부 75g(3/4모), 칵테일새우 20g(5마리), 달걀 30g(1/2개), 생크림 13g(1큰술), 설탕 5g(1작은술), 무염버터 5g(1작은술)\n고명\n시금치 10g(3줄기)",
                        List.of("연두부", "칵테일새우", "달걀", "생크림", "설탕", "버터", "시금치")),
                // [1인분] 머리말, "·양념장 :" 머리말, 수식어(다진·저염), "약간", 분수 문자(⅓)
                Arguments.of("[1인분]조선부추 50g, 날콩가루 7g(1⅓작은술)\n·양념장 : 저염간장 3g(2/3작은술), 다진 대파 5g(1작은술), 다진 마늘 2g(1/2쪽), 참깨 약간",
                        List.of("조선부추", "날콩가루", "간장", "대파", "마늘", "참깨")),
                // "●… : " 머리말 줄, 크기 표기(3×1cm)
                Arguments.of("●방울토마토 소박이 : \n방울토마토 150g(5개), 양파 10g(3×1cm), 부추 10g(5줄기)\n●양념장 : \n고춧가루 4g(1작은술), 통깨 약간",
                        List.of("방울토마토", "양파", "부추", "고춧가루", "통깨")),
                // "•필수 재료 :", 이름 바로 뒤 괄호 수량, 같은 레시피 안 중복 이름
                Arguments.of("•필수 재료 : 표고버섯(3g), 애호박(10g)\n•육수 : 다시마(3g), 표고버섯 밑동(3g), 물(250g)\n•양념 : 저염된장(5g), 다진마늘(1g)",
                        List.of("표고버섯", "애호박", "다시마", "표고버섯밑동", "물", "된장", "마늘")),
                // 줄 앞 "재료" 머리말, 괄호 안 쉼표
                Arguments.of("재료 바나나(150g), 저염베이컨(46g, 3장)\n우유(200g), 소금(2g)",
                        List.of("바나나", "베이컨", "우유", "소금")),
                // 단위 없는 숫자, 이름 중간 괄호 설명
                Arguments.of("밥 180, 배추김치(줄기부분) 30, 생 표고버섯 40, 피자치즈(모짜렐라) 30",
                        List.of("밥", "배추김치", "표고버섯", "피자치즈")),
                // 빈 조각(", ,"), 쉼표 뒤 공백 없음, ml
                Arguments.of("참외 20g, 설탕 3g, , 누룩소금 0.5g, 물 150ml,저염된장 4g",
                        List.of("참외", "설탕", "누룩소금", "물", "된장")),
                // 대괄호 머리말 공백, ½ 분수, 크기 표기 괄호
                Arguments.of("[ 2인분 ] 마늘(4쪽), 육수용 멸치(½컵), 다시마(5×5cm, 2장)",
                        List.of("마늘", "육수용멸치", "다시마")),
                // 줄 끝 쉼표로 이어지는 여러 줄
                Arguments.of("바게트(1/4개), 새송이버섯(15g),\n표고버섯(10g), 올리브오일(10g)",
                        List.of("바게트", "새송이버섯", "표고버섯", "올리브오일")),
                // "생강"은 "생 " 수식어로 잘리지 않는다
                Arguments.of("생강 1g, 생 강황 2g",
                        List.of("생강", "강황")),
                // HTML 줄바꿈, 줄 앞·중간의 ">" 구분
                Arguments.of("토마토(60g)<br>> 크림치즈 90g<br />그릭요거트 토핑 > 그릭요거트 42g",
                        List.of("토마토", "크림치즈", "그릭요거트")),
                // 줄 중간 대괄호 머리말, 붙어 있는 "적당량", 조각 안 콜론
                Arguments.of("소금적당량[멸치소스]멸치액젓 2g, 파슬리 1g[소스소개]비트양념:식초 15g",
                        List.of("소금", "멸치액젓", "파슬리", "식초")),
                // 콜론 없는 소제목, 뒤에 붙은 "다진것"·"불린것"·"장식용"·마침표
                Arguments.of("조림양념 저염간장(3g), 반죽양념 다진 파(10g), 마늘다진것 1.2g, 시래기불린것 10g, 민트장식용, 겨자가루(10g).",
                        List.of("간장", "파", "마늘", "시래기", "민트", "겨자가루")),
                // 머리말 단어만 남는 조각은 버린다
                Arguments.of("[양념] 간장 5g\n소금 1g, [소스], 설탕 2g",
                        List.of("간장", "소금", "설탕"))
        );
    }

    @ParameterizedTest
    @MethodSource("samples")
    void 재료_문자열을_이름_목록으로_나눈다(String parts, List<String> expected) {
        assertThat(names(parts)).containsExactlyElementsOf(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\n"})
    void 빈_입력은_빈_목록(String parts) {
        assertThat(parser.parse(parts)).isEmpty();
    }

    @Test
    void 원문_조각을_보관한다() {
        assertThat(parser.parse("다진 마늘 2g(1/2쪽), 참깨 약간"))
                .extracting(ParsedIngredient::rawText)
                .containsExactly("다진 마늘 2g(1/2쪽)", "참깨 약간");
    }

    @Test
    void 동의어_사전으로_일반적인_이름_하나로_맞춘다() {
        RecipeIngredientParser withAliases = new RecipeIngredientParser(Map.of("달걀", "계란", "통깨", "참깨"));

        assertThat(withAliases.parse("달걀 30g, 통깨 약간, 참깨 1g"))
                .extracting(ParsedIngredient::name)
                .containsExactly("계란", "참깨"); // 같은 이름이 되면 하나로
    }

    @Test
    void 기준_이름에서_모든_별칭을_찾을_수_있다() { // D-026: 별칭을 지우지 않고 둘 다 찾힌다
        RecipeIngredientParser p = new RecipeIngredientParser(List.of(
                new RecipeIngredientParser.AliasEntry("계란", "달걀", true),
                new RecipeIngredientParser.AliasEntry("삶은달걀", "달걀", false),
                new RecipeIngredientParser.AliasEntry("절임물", RecipeIngredientParser.IGNORE, false)));

        assertThat(p.namesOf("계란")).containsExactly("달걀", "계란", "삶은달걀");
        assertThat(p.namesOf("달걀")).containsExactly("달걀", "계란", "삶은달걀");
        assertThat(p.namesOf("두부")).containsExactly("두부"); // 사전에 없는 이름은 자기 자신만
        assertThat(p.namesOf("절임물")).isEmpty();
    }

    @Test
    void 둘_다_흔한_별칭만_표시용으로_돌려준다() {
        RecipeIngredientParser p = new RecipeIngredientParser(List.of(
                new RecipeIngredientParser.AliasEntry("계란", "달걀", true),
                new RecipeIngredientParser.AliasEntry("삶은달걀", "달걀", false)));

        assertThat(p.commonAliasesOf("계란")).containsExactly("계란");
        assertThat(p.commonAliasesOf("두부")).isEmpty();
    }

    @Test
    void 리소스_사전의_both_표시를_읽는다() {
        RecipeIngredientParser fromResource = new RecipeIngredientParser();

        assertThat(fromResource.commonAliasesOf("달걀")).contains("계란");
        assertThat(fromResource.namesOf("계란")).contains("달걀", "계란");
    }

    @Test
    void 사전에서_무시로_표시한_이름은_버린다() {
        RecipeIngredientParser withAliases = new RecipeIngredientParser(Map.of("절임물", RecipeIngredientParser.IGNORE));

        assertThat(withAliases.parse("소금 1g, 절임물 100g, 멸치액젓 2g"))
                .extracting(ParsedIngredient::name)
                .containsExactly("소금", "멸치액젓");
    }

    @Test
    void 냉장고_재료_이름도_같은_규칙으로_정규화한다() {
        RecipeIngredientParser withAliases = new RecipeIngredientParser(Map.of("달걀", "계란"));

        assertThat(withAliases.normalize(" 달걀 ")).isEqualTo("계란");
        assertThat(withAliases.normalize("다진 마늘")).isEqualTo("마늘");
        assertThat(withAliases.normalize("방울 토마토")).isEqualTo("방울토마토");
    }

    @Test
    void 기본_생성자는_리소스의_동의어_사전을_읽는다() {
        // 이유 열이 있는 행도 읽는다 (사전: 계란→달걀, 후춧가루→후추)
        RecipeIngredientParser fromResource = new RecipeIngredientParser();
        assertThat(fromResource.normalize("계란")).isEqualTo("달걀");
        assertThat(fromResource.normalize("후춧가루")).isEqualTo("후추");
    }
}
