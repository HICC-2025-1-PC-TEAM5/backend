package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.Ingredient;

import java.util.List;

public interface IngredientRepository {

    Ingredient save(Ingredient ingredient);

    List<Ingredient> findAll();

    Ingredient findById(Long id);

    void update(Ingredient ingredient);

    void delete(Ingredient ingredient);

}
