package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.RefridgeDto;
import hicc_project.RottenToday.dto.RefrigeratorIngredientResponse;
import hicc_project.RottenToday.entity.Category;
import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.entity.RefrigeratorIngredient;
import hicc_project.RottenToday.entity.StorageCondition;
import hicc_project.RottenToday.repository.IngredientRepository;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RefrigeratorIngredientRepository;
import hicc_project.RottenToday.service.recipe.RecipeIngredientParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// B24: 소비기한 표 (D-028). 0일은 실온 육류·어패류·유제품만 남고, 사용자가 입력한 소비기한이 계산값보다 우선한다
class IngredientServiceExpireTest {

    @ParameterizedTest(name = "{0} {1} → {2}일")
    @CsvSource({
            "VEGETABLE, NORMAL, 1",    "VEGETABLE, REFRIGERATED, 5",    "VEGETABLE, FROZEN, 25",
            "FRUIT, NORMAL, 5",        "FRUIT, REFRIGERATED, 10",       "FRUIT, FROZEN, 60",
            "GRAIN, NORMAL, 60",       "GRAIN, REFRIGERATED, 120",      "GRAIN, FROZEN, 270",
            "MEAT, NORMAL, 0",         "MEAT, REFRIGERATED, 3",         "MEAT, FROZEN, 240",
            "SEAFOOD, NORMAL, 0",      "SEAFOOD, REFRIGERATED, 2",      "SEAFOOD, FROZEN, 120",
            "EGG, NORMAL, 14",         "EGG, REFRIGERATED, 30",         "EGG, FROZEN, 270",
            "DAIRY, NORMAL, 0",        "DAIRY, REFRIGERATED, 7",        "DAIRY, FROZEN, 45",
            "BEANS, NORMAL, 270",      "BEANS, REFRIGERATED, 5",        "BEANS, FROZEN, 90",
            "OIL, NORMAL, 270",        "OIL, REFRIGERATED, 270",        "OIL, FROZEN, 270",
            "CONDIMENT, NORMAL, 1000", "CONDIMENT, REFRIGERATED, 365",  "CONDIMENT, FROZEN, 365",
            "PROCESSED, NORMAL, 30",   "PROCESSED, REFRIGERATED, 7",    "PROCESSED, FROZEN, 60",
            "DRINK, NORMAL, 135",      "DRINK, REFRIGERATED, 4",        "DRINK, FROZEN, 30",
            "ETC, NORMAL, 5",          "ETC, REFRIGERATED, 10",         "ETC, FROZEN, 60",
    })
    void 카테고리와_보관방식별_소비기한(Category category, StorageCondition condition, int days) {
        assertThat(IngredientService.measureExpireDate(category, condition)).isEqualTo(days);
    }

    @Test
    void 표의_모든_칸이_정의되어_있다() {
        // 0일은 실온 육류·어패류·유제품(당일 처리)만 허용한다. 새 카테고리가 생기면 여기서 걸린다
        List<Category> sameDayAtRoom = List.of(Category.MEAT, Category.SEAFOOD, Category.DAIRY);
        for (Category category : Category.values()) {
            for (StorageCondition condition : StorageCondition.values()) {
                boolean sameDay = condition == StorageCondition.NORMAL && sameDayAtRoom.contains(category);
                int days = IngredientService.measureExpireDate(category, condition);
                if (sameDay) {
                    assertThat(days).as("%s %s", category, condition).isZero();
                } else {
                    assertThat(days).as("%s %s", category, condition).isPositive();
                }
            }
        }
    }

    private RefrigeratorIngredientRepository fridgeRepository;
    private IngredientService ingredientService;

    @BeforeEach
    void setUp() {
        fridgeRepository = mock(RefrigeratorIngredientRepository.class);
        IngredientRepository ingredientRepository = mock(IngredientRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        Member member = new Member();
        member.setId(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(ingredientRepository.findByName(any())).thenReturn(Optional.empty());
        ingredientService = new IngredientService(fridgeRepository, ingredientRepository, memberRepository,
                new RecipeIngredientParser(List.<RecipeIngredientParser.AliasEntry>of()));
    }

    private RefrigeratorIngredient register(LocalDateTime expire) {
        RefridgeDto dto = new RefridgeDto();
        dto.setName("두부");
        dto.setQuantity(1);
        dto.setUnit("모");
        dto.setType("냉장실");
        dto.setCategory("두류/콩류");
        dto.setExpire_date(expire);
        RefrigeratorIngredientResponse request = new RefrigeratorIngredientResponse(List.of(dto));

        ingredientService.addRefridgeIngredient(1L, request);

        ArgumentCaptor<RefrigeratorIngredient> saved = ArgumentCaptor.forClass(RefrigeratorIngredient.class);
        verify(fridgeRepository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void 사용자가_입력한_소비기한을_그대로_쓴다() {
        LocalDateTime printed = LocalDateTime.of(2026, 10, 20, 0, 0);

        assertThat(register(printed).getExpire_date()).isEqualTo(printed);
    }

    @Test
    void 입력이_없으면_카테고리와_보관방식으로_계산한다() {
        RefrigeratorIngredient saved = register(null);

        // 두류 냉장 5일 (B24 전에는 0일이라 등록 당일 만료)
        assertThat(Duration.between(saved.getInput_date(), saved.getExpire_date()).toDays()).isEqualTo(5);
    }
}
