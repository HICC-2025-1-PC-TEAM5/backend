package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.*;
import hicc_project.RottenToday.entity.*;
import hicc_project.RottenToday.repository.HistoryRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
import hicc_project.RottenToday.repository.TasteRepository;
import hicc_project.RottenToday.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    public UserService(TasteRepository tasteRepository, RecipeRepository recipeRepository, HistoryRepository historyRepository, MemberRepository memberRepository) {
        this.tasteRepository = tasteRepository;
        this.recipeRepository = recipeRepository;
        this.historyRepository = historyRepository;
        this.memberRepository = memberRepository;
    }

    public TasteRecipeListResponse getTaste(Long userId) {
        List<Recipe> recipes = tasteRepository.findByMemberId(userId);
        List<TasteRecipeResponse> tasteRecipeResponseList = new ArrayList<>();
        for (Recipe recipe : recipes) {
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

    public HistoryListResponse getHistory(Long userId) {
        List<History> histories = historyRepository.findByMemberId(userId);
        HistoryListResponse historyListResponse = new HistoryListResponse(histories);
        return historyListResponse;
    }

    public void updateHistory(Long userId, Long recipeId) {
        LocalDateTime time = LocalDateTime.now();
        Member member = memberRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 레시피가 존재하지 않습니다."));
        History history = new History(member, time, recipe);
        historyRepository.save(history);
    }

    public HistoryListResponse getFavorites(Long userId) {
        List<History> favorites = historyRepository.findByMemberId(userId).stream()
                .filter(f ->f.isFavorite())
                .collect(Collectors.toList());
        HistoryListResponse historyListResponse = new HistoryListResponse(favorites);
        return historyListResponse;
    }


    public void updateFavorites(Long userId, FavoriteRequestDto requestDto) {
        History history = historyRepository.findById(requestDto.getRecipeId()).orElseThrow(() -> new EntityNotFoundException("해당 레시피가 존재하지 않습니다."));
        history.setFavorite(requestDto.isType());
        historyRepository.save(history);

    }
}
