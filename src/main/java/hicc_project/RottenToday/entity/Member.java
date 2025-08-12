package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String email;

    private String name;
    private String picture;

    @OneToMany(mappedBy = "member")
    private List<History> cookHistories = new ArrayList<>();
    @Column(nullable=false)
    private String provider;      // "google"

    @OneToMany(mappedBy = "member")
    private List<Taste> tastes = new ArrayList<>();
    @Column(nullable=false, unique=true)
    private String externalId;    // 구글 userinfo id(or sub)

    @OneToMany(mappedBy = "member")
    private List<Allergy> allergies = new ArrayList<>();
    @Column(nullable=false)
    private long tokenVersion;    // 로그아웃 시 ++

    @OneToMany(mappedBy = "member")
    private List<RefrigeratorIngredient> refrigeratorIngredients = new ArrayList<>();
    public void bumpTokenVersion() { this.tokenVersion++; }
}
