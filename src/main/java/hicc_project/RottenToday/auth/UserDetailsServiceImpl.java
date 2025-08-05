package hicc_project.RottenToday.auth;

import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        Member member = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다: " + email));

        return new User(
                member.getEmail(),
                "", // 비밀번호는 사용하지 않음 (OAuth2 로그인)
                Collections.singleton(() -> "ROLE_USER")
        );
    }
}
