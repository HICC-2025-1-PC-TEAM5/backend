package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Ingredient;
import hicc_project.RottenToday.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

}
