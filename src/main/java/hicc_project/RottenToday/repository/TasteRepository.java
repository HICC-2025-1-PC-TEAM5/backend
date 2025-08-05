package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Taste;
import hicc_project.RottenToday.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TasteRepository extends JpaRepository<Taste, Long> {


    List<Taste> findByUsersId(Long userId);

    @Query("SELECT t.recipe FROM Taste t WHERE t.users.id = :userId")
    List<Recipe> findRecipesByUserId(@Param("userId") Long userId);
}