package hicc_project.RottenToday.service;

import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.Taste;
import hicc_project.RottenToday.exception.DuplicateEntityException;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
import hicc_project.RottenToday.repository.RecipeStepRepository;
import hicc_project.RottenToday.repository.TasteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// B4: 레시피 좋아요/싫어요 중복 검사는 회원 범위로 한다
class RecipeServiceFavoriteTest {

    private TasteRepository tasteRepository;
    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        tasteRepository = mock(TasteRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        recipeService = new RecipeService(recipeRepository, tasteRepository, memberRepository, mock(RecipeStepRepository.class));

        Member member = new Member();
        member.setId(3L);
        when(memberRepository.findById(3L)).thenReturn(Optional.of(member));
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(new Recipe()));
    }

    @Test
    void 다른_회원이_같은_레시피를_좋아요했어도_저장된다() {
        when(tasteRepository.findByRecipeIdAndMemberId(1L, 3L)).thenReturn(Optional.empty());

        recipeService.addfavorite(3L, 1L, "좋아요");

        verify(tasteRepository).findByRecipeIdAndMemberId(1L, 3L);
        verify(tasteRepository).save(any(Taste.class));
    }

    @Test
    void 같은_회원이_같은_레시피를_다시_등록하면_409() {
        when(tasteRepository.findByRecipeIdAndMemberId(1L, 3L)).thenReturn(Optional.of(mock(Taste.class)));

        assertThatThrownBy(() -> recipeService.addfavorite(3L, 1L, "좋아요"))
                .isInstanceOf(DuplicateEntityException.class);
        verify(tasteRepository, never()).save(any());
    }
}
