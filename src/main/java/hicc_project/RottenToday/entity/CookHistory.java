package hicc_project.RottenToday.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class CookHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private LocalDateTime viewAt;
    private boolean favorite;

    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

}
