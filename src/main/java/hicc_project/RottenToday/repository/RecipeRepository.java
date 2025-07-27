package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Ingredient;
import hicc_project.RottenToday.entity.Recipe;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository {

    Recipe save(Recipe recipe);

    void update(Recipe recipe);

    List<Recipe> findAll();

    Optional<Recipe> findById(Long id);

    void deleteById(Long id);

}
