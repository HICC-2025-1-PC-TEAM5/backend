package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.TasteRecipeDto;
import hicc_project.RottenToday.dto.TasteRecipeListResponse;
import hicc_project.RottenToday.dto.TasteRecipeResponse;
import hicc_project.RottenToday.entity.Appetite;
import hicc_project.RottenToday.entity.Recipe;
import hicc_project.RottenToday.entity.Taste;
import hicc_project.RottenToday.repository.RecipeRepository;
import hicc_project.RottenToday.repository.TasteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    TasteRepository tasteRepository;
    RecipeRepository recipeRepository;

    @Autowired
    public UserService(TasteRepository tasteRepository, RecipeRepository recipeRepository) {
        this.tasteRepository = tasteRepository;
        this.recipeRepository = recipeRepository;
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
}
