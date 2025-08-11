// src/main/java/hicc_project/RottenToday/entity/Member.java
package hicc_project.RottenToday.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable=false)
    private String provider;      // "google"

    @Column(nullable=false, unique=true)
    private String externalId;    // 구글 userinfo id(or sub)

    @Column(nullable=false)
    private long tokenVersion;    // 로그아웃 시 ++

    public void bumpTokenVersion() { this.tokenVersion++; }
}
