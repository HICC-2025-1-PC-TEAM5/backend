package hicc_project.RottenToday.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private String email;

    private String token;

    public void updateToken(String newToken) {
        this.token = newToken;
    }
}
