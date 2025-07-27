package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.HistoryListResponse;
import hicc_project.RottenToday.dto.TasteRecipeDto;
import hicc_project.RottenToday.dto.TasteRecipeListResponse;
import hicc_project.RottenToday.dto.TasteRecipeResponse;
import hicc_project.RottenToday.entity.*;
import hicc_project.RottenToday.repository.HistoryRepository;
import hicc_project.RottenToday.repository.RecipeRepository;
import hicc_project.RottenToday.repository.TasteRepository;
import hicc_project.RottenToday.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    TasteRepository tasteRepository;
    RecipeRepository recipeRepository;
    HistoryRepository historyRepository;

    @Autowired
    public UserService(TasteRepository tasteRepository, RecipeRepository recipeRepository, HistoryRepository historyRepository, UserRepository userRepository) {
        this.tasteRepository = tasteRepository;
        this.recipeRepository = recipeRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    public TasteRecipeListResponse getTaste(Long userId) {
        List<Recipe> recipes = tasteRepository.findByUserId(userId);
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
        List<History> histories = historyRepository.findByUserId(userId);
        HistoryListResponse historyListResponse = new HistoryListResponse(histories);
        return historyListResponse;
    }

    public void updateHistory(Long userId, Long recipeId) {
        LocalDateTime time = LocalDateTime.now();
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("해당 유저가 존재하지 않습니다."));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 레시피가 존재하지 않습니다."));
        History history = new History(user, time, recipe);
        historyRepository.save(history);
    }
}
