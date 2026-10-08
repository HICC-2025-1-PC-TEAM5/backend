package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    @Query("SELECT i FROM Ingredient i WHERE i.name = :name")
    Optional<Ingredient> findByName(String name);

    // 여러 이름을 한 번에 조회 (기본 재료 추천, 이름마다 조회하던 것을 묶음)
    List<Ingredient> findAllByNameIn(Collection<String> names);
}
