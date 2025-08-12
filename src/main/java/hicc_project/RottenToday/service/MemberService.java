package hicc_project.RottenToday.service;

import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;   // ★ 추가
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository repo;

    @Transactional
    public Member upsertGoogleUser(String externalId, String email, String name, String picture) {
        return repo.findByProviderAndExternalId("google", externalId)
                .map(m -> {
                    m.setEmail(email);
                    m.setName(name);
                    m.setPicture(picture);
                    return m;
                })
                .orElseGet(() -> repo.save(
                        Member.builder()
                                .provider("google")
                                .externalId(externalId)
                                .email(email)
                                .name(name)
                                .picture(picture)
                                .tokenVersion(0L)
                                .build()
                ));
    }

    @Transactional(readOnly = true)
    public long getTokenVersion(long memberId) {
        return repo.findById(memberId).map(Member::getTokenVersion).orElse(0L);
    }

    @Transactional
    public void bumpTokenVersion(long memberId) {
        repo.findById(memberId).ifPresent(Member::bumpTokenVersion);
    }

    // ★ 추가: AuthenticationPrincipal로 쓸 Member 로드
    @Transactional(readOnly = true)
    public Member getById(long memberId) {
        return repo.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("member not found: " + memberId));
    }
}
