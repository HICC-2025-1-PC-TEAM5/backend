package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.IngredientDto;
import hicc_project.RottenToday.entity.Allergy;
import hicc_project.RottenToday.entity.Ingredient;
import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.exception.DuplicateEntityException;
import hicc_project.RottenToday.exception.NoInputException;
import hicc_project.RottenToday.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// B4: 알레르기 중복 검사는 회원 범위로 한다
class UserServiceAllergyTest {

    private AllergyRepository allergyRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        allergyRepository = mock(AllergyRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        IngredientRepository ingredientRepository = mock(IngredientRepository.class);
        userService = new UserService(mock(TasteRepository.class), mock(RecipeRepository.class),
                mock(HistoryRepository.class), memberRepository, allergyRepository, ingredientRepository);

        Member member = new Member();
        member.setId(2L);
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member));
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(new Ingredient()));
    }

    private static IngredientDto request(long ingredientId) {
        IngredientDto dto = new IngredientDto();
        dto.setIngredientId(ingredientId);
        return dto;
    }

    @Test
    void 다른_회원이_같은_재료를_등록했어도_저장된다() {
        when(allergyRepository.existsByMemberIdAndIngredientId(2L, 1L)).thenReturn(false);

        userService.addAllergy(2L, request(1L));

        verify(allergyRepository).existsByMemberIdAndIngredientId(2L, 1L);
        verify(allergyRepository).save(any(Allergy.class));
    }

    @Test
    void 같은_회원이_같은_재료를_다시_등록하면_409() {
        when(allergyRepository.existsByMemberIdAndIngredientId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.addAllergy(2L, request(1L)))
                .isInstanceOf(DuplicateEntityException.class);
        verify(allergyRepository, never()).save(any());
    }

    @Test
    void 재료_id가_없으면_400() { // B25: 전에는 findById(null)로 500
        assertThatThrownBy(() -> userService.addAllergy(2L, new IngredientDto()))
                .isInstanceOf(NoInputException.class)
                .hasMessage("알레르기로 등록할 재료를 선택해 주세요.");
        verify(allergyRepository, never()).save(any());
    }

    @Test
    void 없는_재료_id면_404() {
        assertThatThrownBy(() -> userService.addAllergy(2L, request(999L)))
                .isInstanceOf(EntityNotFoundException.class);
        verify(allergyRepository, never()).save(any());
    }
}
