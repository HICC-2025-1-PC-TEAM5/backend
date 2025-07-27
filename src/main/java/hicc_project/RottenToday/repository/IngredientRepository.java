package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

}
