package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.TasteRecipeDto;
import hicc_project.RottenToday.dto.TasteRecipeListResponse;
import hicc_project.RottenToday.dto.TasteRecipeResponse;
import hicc_project.RottenToday.entity.Appetite;
import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.Taste;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
import hicc_project.RottenToday.repository.TasteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final TasteRepository tasteRepository;
    private final RecipeRepository recipeRepository;
    private final MemberRepository memberRepository; // 추가됨

    public TasteRecipeListResponse getTaste(Long userId) {
        List<Taste> tastes = tasteRepository.findByUsersId(userId); // 수정됨
        List<TasteRecipeResponse> tasteRecipeResponseList = new ArrayList<>();

        for (Taste taste : tastes) {
            Recipe recipe = taste.getRecipe();
            TasteRecipeDto tasteRecipeDto = new TasteRecipeDto(recipe);
            tasteRecipeResponseList.add(new TasteRecipeResponse(tasteRecipeDto));
        }

        return new TasteRecipeListResponse(tasteRecipeResponseList);
    }

    public void updateTaste(Long recipeId, Appetite type) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 레시피가 존재하지 않습니다."));

        Taste taste = new Taste(recipeId, type, recipe);
        tasteRepository.save(taste);
    }

    public void deleteTaste(Long tasteId) {
        tasteRepository.deleteById(tasteId);
    }

    // 회원 탈퇴 기능 추가
    public void deleteMember(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        memberRepository.delete(member);
    }
}
