package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.*;
import hicc_project.RottenToday.entity.*;
import hicc_project.RottenToday.exception.DuplicateEntityException;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
import hicc_project.RottenToday.repository.RecipeStepRepository;
import hicc_project.RottenToday.repository.TasteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

// 레시피 상세·좋아요/싫어요. 추천은 RecipeRecommendService가 로컬 DB에서 한다 (레시피 계획 Phase 5, 외부 API 호출 제거)
@Slf4j
@Service
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final MemberRepository memberRepository;
    private final TasteRepository tasteRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeRecommendService recipeRecommendService; // 상세의 지난 재료·대체 가능 재료 계산 (D-038·D-040)

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, TasteRepository tasteRepository, MemberRepository memberRepository, RecipeStepRepository recipeStepRepository, RecipeRecommendService recipeRecommendService) {
        this.recipeRepository = recipeRepository;
        this.tasteRepository = tasteRepository;
        this.memberRepository = memberRepository;
        this.recipeStepRepository = recipeStepRepository;
        this.recipeRecommendService = recipeRecommendService;
    }

    public RecipeDetailResponse getRecipeDetail(Long userId, Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow(() -> new EntityNotFoundException("해당 레시피 존재 x"));
        List<RecipeStep> byRecipeId = recipeStepRepository.findByRecipeId(recipeId);
        RecipeGuide recipeGuide = new RecipeGuide(byRecipeId);
        // 어느 화면에서 열어도 지난 재료·대체 가능 안내가 보이게 상세 응답에 넣는다 (D-038·D-040)
        RecipeRecommendService.DetailNotes notes = recipeRecommendService.detailNotesOf(userId, recipeId);
        RecipeDetailResponse response = new RecipeDetailResponse(recipe, recipeGuide, notes.expiredIngredients(), notes.substitutes());
        return response;
    }

    public void addfavorite(Long userId, Long recipeId, String appetite) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 레시피 존재 x"));
        Appetite.fromStatus(appetite); // 좋아요/싫어요가 아니면 400
        if (tasteRepository.findByRecipeIdAndMemberId(recipeId, userId).isPresent()){
            throw new DuplicateEntityException("이미 해당 레시피를 저장하였습니다.");
        }
        Taste taste = new Taste(appetite, recipe, member);
        tasteRepository.save(taste);

    }

}
