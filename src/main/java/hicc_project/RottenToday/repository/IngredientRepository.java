package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    @Query("SELECT i FROM Ingredient i WHERE i.name = :name")
    Optional<Ingredient> findByName(String name);
}
