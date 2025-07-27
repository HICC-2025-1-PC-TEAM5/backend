package hicc_project.RottenToday.entity;

import jakarta.persistence.*;

@Entity
public class Taste {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Appetite type;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;
}
