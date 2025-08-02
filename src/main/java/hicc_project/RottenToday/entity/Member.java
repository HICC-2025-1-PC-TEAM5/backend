package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
@Entity
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "member")
    private List<History> cookHistories = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<Taste> tastes = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<Allergy> allergies = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<RefrigeratorIngredient> refrigeratorIngredients = new ArrayList<>();
}
