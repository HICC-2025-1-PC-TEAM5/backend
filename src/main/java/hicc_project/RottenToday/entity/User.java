package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
@Entity
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "user")
    private List<History> cookHistories = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Taste> tastes = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Allergy> allergies = new ArrayList<>();

    @OneToMany
    private List<RefrigeratorIngredient> refrigeratorIngredients = new ArrayList<>();
}
