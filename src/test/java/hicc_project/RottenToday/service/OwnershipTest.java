package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.FavoriteRequestDto;
import hicc_project.RottenToday.dto.RefridgeIngredientRequest;
import hicc_project.RottenToday.entity.*;
import hicc_project.RottenToday.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// B5: 하위 데이터(냉장고 재료·알레르기·기록)는 주인만 조회·수정·삭제할 수 있고, 남의 것이면 404 (D-012)
class OwnershipTest {

    private static final long OWNER = 1L;
    private static final long OTHER = 2L;

    private RefrigeratorIngredientRepository fridgeRepository;
    private AllergyRepository allergyRepository;
    private HistoryRepository historyRepository;
    private IngredientService ingredientService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        fridgeRepository = mock(RefrigeratorIngredientRepository.class);
        allergyRepository = mock(AllergyRepository.class);
        historyRepository = mock(HistoryRepository.class);
        ingredientService = new IngredientService(fridgeRepository, mock(IngredientRepository.class), mock(MemberRepository.class));
        userService = new UserService(mock(TasteRepository.class), mock(RecipeRepository.class), historyRepository,
                mock(MemberRepository.class), allergyRepository, mock(IngredientRepository.class));
    }

    private static Member member(long id) {
        Member m = new Member();
        m.setId(id);
        return m;
    }

    private static RefrigeratorIngredient fridgeItem(long id, long ownerId) {
        RefrigeratorIngredient item = new RefrigeratorIngredient();
        item.setId(id);
        item.setMember(member(ownerId));
        item.setQuantity(3);
        item.setType(StorageCondition.REFRIGERATED);
        item.setCategory(Category.VEGETABLE);
        return item;
    }

    private static RefridgeIngredientRequest quantityRequest(long id, int quantity) {
        RefridgeIngredientRequest request = new RefridgeIngredientRequest();
        ReflectionTestUtils.setField(request, "refrigeratorIngredientId", id);
        ReflectionTestUtils.setField(request, "quantity", quantity);
        return request;
    }

    // --- 냉장고 재료 ---

    @Test
    void 본인_재료_수량은_수정된다() {
        RefrigeratorIngredient item = fridgeItem(10L, OWNER);
        when(fridgeRepository.findById(10L)).thenReturn(Optional.of(item));

        ingredientService.updateRefridgeIngredient(OWNER, quantityRequest(10L, 5));

        assertThat(item.getQuantity()).isEqualTo(5);
    }

    @Test
    void 남의_재료_수량을_수정하면_404이고_바뀌지_않는다() {
        RefrigeratorIngredient item = fridgeItem(10L, OWNER);
        when(fridgeRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> ingredientService.updateRefridgeIngredient(OTHER, quantityRequest(10L, 0)))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(item.getQuantity()).isEqualTo(3);
        verify(fridgeRepository, never()).delete(any());
    }

    @Test
    void 남의_재료나_없는_재료를_삭제하면_404() {
        when(fridgeRepository.findByMemberId(OTHER)).thenReturn(List.of());

        assertThatThrownBy(() -> ingredientService.deleteIngredient(OTHER, 10L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(fridgeRepository, never()).delete(any());
    }

    @Test
    void 본인_재료는_삭제된다() {
        RefrigeratorIngredient item = fridgeItem(10L, OWNER);
        when(fridgeRepository.findByMemberId(OWNER)).thenReturn(List.of(item));

        ingredientService.deleteIngredient(OWNER, 10L);

        verify(fridgeRepository).delete(item);
    }

    @Test
    void 남의_재료를_단건_조회하면_404() {
        when(fridgeRepository.findById(10L)).thenReturn(Optional.of(fridgeItem(10L, OWNER)));

        assertThatThrownBy(() -> ingredientService.getRefridgeIngredient(OTHER, 10L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 본인_재료는_단건_조회된다() {
        when(fridgeRepository.findById(10L)).thenReturn(Optional.of(fridgeItem(10L, OWNER)));

        assertThat(ingredientService.getRefridgeIngredient(OWNER, 10L).getId()).isEqualTo(10L);
    }

    // --- 알레르기 ---

    @Test
    void 남의_알레르기를_삭제하면_404이고_지워지지_않는다() {
        Allergy allergy = new Allergy(member(OWNER), new Ingredient());
        allergy.setId(20L);
        when(allergyRepository.findById(20L)).thenReturn(Optional.of(allergy));

        assertThatThrownBy(() -> userService.deleteAllergy(OTHER, 20L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(allergyRepository, never()).delete(any());
        verify(allergyRepository, never()).deleteById(any());
    }

    @Test
    void 본인_알레르기는_삭제된다() {
        Allergy allergy = new Allergy(member(OWNER), new Ingredient());
        allergy.setId(20L);
        when(allergyRepository.findById(20L)).thenReturn(Optional.of(allergy));

        userService.deleteAllergy(OWNER, 20L);

        verify(allergyRepository).delete(allergy);
    }

    // --- 기록(즐겨찾기) ---

    @Test
    void 남의_기록을_즐겨찾기하면_404이고_바뀌지_않는다() {
        History history = new History(member(OWNER), LocalDateTime.now(), new Recipe());
        when(historyRepository.findById(30L)).thenReturn(Optional.of(history));
        FavoriteRequestDto request = new FavoriteRequestDto();
        ReflectionTestUtils.setField(request, "historyId", 30L);
        ReflectionTestUtils.setField(request, "type", true);

        assertThatThrownBy(() -> userService.updateFavorites(OTHER, request))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(history.isFavorite()).isFalse();
    }
}
