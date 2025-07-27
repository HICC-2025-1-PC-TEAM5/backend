package hicc_project.RottenToday.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    @OneToMany(mappedBy = "user")
    private List<CookHistory> cookHistories;

    @OneToMany(mappedBy = "user")
    private List<Taste> tastes;

    @OneToMany(mappedBy = "user")
    private List<Allergy> allergies;

    @OneToMany
    private List<RefrigeratorIngredient> refrigeratorIngredients;
}
