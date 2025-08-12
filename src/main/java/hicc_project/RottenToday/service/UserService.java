package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.*;
import hicc_project.RottenToday.entity.*;
import hicc_project.RottenToday.repository.*;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final MemberRepository memberRepository;
    TasteRepository tasteRepository;
    RecipeRepository recipeRepository;
    HistoryRepository historyRepository;
    AllergyRepository allergyRepository;
    IngredientRepository ingredientRepository;

    @Autowired
    public UserService(TasteRepository tasteRepository, RecipeRepository recipeRepository, HistoryRepository historyRepository, MemberRepository memberRepository, AllergyRepository allergyRepository, IngredientRepository ingredientRepository) {
        this.tasteRepository = tasteRepository;
        this.recipeRepository = recipeRepository;
        this.historyRepository = historyRepository;
        this.memberRepository = memberRepository;
        this.allergyRepository = allergyRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public TasteRecipeResponse getTaste(Long userId) {
        List<Taste> recipes = tasteRepository.findByMemberId(userId);
        List<TasteRecipeDto> response = new ArrayList<>();
        for (Taste recipe : recipes) {
            response.add(new TasteRecipeDto(recipe.getRecipe()));
        }
        return new TasteRecipeResponse(response);
    }

    public void updateTaste(Long userId, Long recipeId, String appetite) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 레시피가 존재하지 않습니다."));
        recipe.setUsed(true); //레시피 사용된거는 디비에서 안지워지게 설정하기 위함
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
        if (!(appetite.equals("좋아요") || appetite.equals("싫어요"))){
            throw new IllegalArgumentException("type 변수값으로 '좋아요' 혹은 '싫어요' 입력할 수 있습니다.");
        }
        Taste taste = new Taste(appetite, recipe, member);
        tasteRepository.save(taste);
    }

    public void deleteTaste(Long tasteId) {
        tasteRepository.deleteById(tasteId);
    }

    public HistoryListResponse getHistory(Long userId) {
        List<History> histories = historyRepository.findByMemberId(userId);
        List<HistoryDto> historyDtoList = new ArrayList<>();
        for (History history : histories) {
            HistoryDto historyDto = new HistoryDto(history);
            historyDtoList.add(historyDto);
        }
        HistoryListResponse historyListResponse = new HistoryListResponse(historyDtoList);
        return historyListResponse;
    }

    @Transactional
    public HistoryDto updateHistory(Long userId, Long recipeId) {
        LocalDateTime time = LocalDateTime.now();
        Member member = memberRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 레시피가 존재하지 않습니다."));
        recipe.setUsed(true);   //레시피 사용된거는 디비에서 안지워지게 설정하기 위함
        History history = new History(member, time, recipe);
        History save = historyRepository.save(history);
        HistoryDto historyDto = new HistoryDto(save);
        return historyDto;
    }

    public HistoryListResponse getFavorites(Long userId) {
        List<History> favorites = historyRepository.findByMemberId(userId).stream()
                .filter(f ->f.isFavorite())
                .collect(Collectors.toList());
        List<HistoryDto> historyDtoList = new ArrayList<>();
        for (History history : favorites) {
            HistoryDto historyDto = new HistoryDto(history);
            historyDtoList.add(historyDto);
        }
        HistoryListResponse historyListResponse = new HistoryListResponse(historyDtoList);
        return historyListResponse;
    }


    public void updateFavorites(Long userId, FavoriteRequestDto requestDto) {
        History history = historyRepository.findById(requestDto.getHistoryId()).orElseThrow(() -> new EntityNotFoundException("해당 레시피가 존재하지 않습니다."));
        history.setFavorite(requestDto.isType());
        historyRepository.save(history);

    }

    public AllergyListResponse getAllergy(Long userId) {
        List<AllergyResponse> allergyResponseList = new ArrayList<>();
        for (Allergy allergy : allergyRepository.findByMemberId(userId)) {
            AllergyResponse allergyResponse = new AllergyResponse(allergy);
            allergyResponseList.add(allergyResponse);
        }


        return new AllergyListResponse(allergyResponseList);
    }

    public void addAllergy(Long userId, IngredientDto request) {
        Member member = memberRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다"));
        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId()).orElseThrow(() -> new EntityNotFoundException("해당 재료를 찾을 수 없습니다"));
        Allergy allergy = new Allergy(member, ingredient);
        allergyRepository.save(allergy);
    }

    public void deleteAllergy(Long allergyId) {
        if (allergyRepository.existsById(allergyId)) {
            allergyRepository.deleteById(allergyId);
        } else {
            throw new EntityNotFoundException("해당 알러지 정보가 존재하지 않습니다.");
        }
    }

    // 회원 탈퇴 기능 추가
    public void deleteMember(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        memberRepository.delete(member);
    }
}


