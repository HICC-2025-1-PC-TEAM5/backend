package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.RefridgeDto;
import hicc_project.RottenToday.dto.RefrigeratorIngredientResponse;
import hicc_project.RottenToday.entity.Category;
import hicc_project.RottenToday.entity.Ingredient;
import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.entity.RefrigeratorIngredient;
import hicc_project.RottenToday.repository.IngredientRepository;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RefrigeratorIngredientRepository;
import hicc_project.RottenToday.service.recipe.RecipeIngredientParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// B23: 냉장고 등록 시 이름이 마스터와 달라도(계란 → 달걀) 마스터에 연결하고 마스터 카테고리를 쓴다. 저장 이름은 사용자가 쓴 그대로 (D-029)
class IngredientServiceMasterLinkTest {

    private RefrigeratorIngredientRepository fridgeRepository;
    private IngredientRepository ingredientRepository;
    private IngredientService ingredientService;

    @BeforeEach
    void setUp() {
        fridgeRepository = mock(RefrigeratorIngredientRepository.class);
        ingredientRepository = mock(IngredientRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        Member member = new Member();
        member.setId(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(ingredientRepository.findByName(anyString())).thenReturn(Optional.empty());
        master("달걀", Category.EGG);
        master("새우", Category.SEAFOOD);
        master("계란빵", Category.PROCESSED);

        RecipeIngredientParser parser = new RecipeIngredientParser(List.of(
                new RecipeIngredientParser.AliasEntry("계란", "달걀", true)));
        ingredientService = new IngredientService(fridgeRepository, ingredientRepository, memberRepository, parser);
    }

    private void master(String name, Category category) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setCategory(category);
        when(ingredientRepository.findByName(name)).thenReturn(Optional.of(ingredient));
    }

    private RefrigeratorIngredient register(String name, String category) {
        RefridgeDto dto = new RefridgeDto();
        dto.setName(name);
        dto.setQuantity(1);
        dto.setUnit("개");
        dto.setType("냉장실");
        dto.setCategory(category);

        ingredientService.addRefridgeIngredient(1L, new RefrigeratorIngredientResponse(List.of(dto)));

        ArgumentCaptor<RefrigeratorIngredient> saved = ArgumentCaptor.forClass(RefrigeratorIngredient.class);
        verify(fridgeRepository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void 동의어로_등록해도_마스터에_연결하고_이름은_그대로_둔다() {
        RefrigeratorIngredient saved = register("계란", null);

        assertThat(saved.getIngredient().getName()).isEqualTo("달걀");
        assertThat(saved.getCategory()).isEqualTo(Category.EGG);
        assertThat(saved.getName()).isEqualTo("계란");
    }

    @Test
    void 수식어가_붙은_이름도_마스터에_연결한다() {
        RefrigeratorIngredient saved = register("냉동 새우", "기타");

        assertThat(saved.getIngredient().getName()).isEqualTo("새우");
        assertThat(saved.getCategory()).isEqualTo(Category.SEAFOOD); // 마스터 카테고리가 보낸 카테고리보다 우선
    }

    @Test
    void 마스터와_정확히_같은_이름은_정규화하지_않는다() {
        RefrigeratorIngredient saved = register("계란빵", null);

        assertThat(saved.getIngredient().getName()).isEqualTo("계란빵");
        verify(ingredientRepository, never()).findByName("달걀");
    }

    @Test
    void 마스터에_없으면_보낸_카테고리를_쓴다() {
        RefrigeratorIngredient saved = register("돼지 목살", "육류");

        assertThat(saved.getIngredient()).isNull();
        assertThat(saved.getCategory()).isEqualTo(Category.MEAT);
    }
}
