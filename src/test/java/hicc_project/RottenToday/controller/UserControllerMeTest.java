package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 구글이 name·picture를 주지 않은 회원도 내 정보 조회가 500이 나지 않는다 (B9 후속)
class UserControllerMeTest {

    @Test
    void 이름과_사진이_없어도_200이고_null로_응답한다() {
        Member member = new Member();
        member.setId(1L);
        member.setEmail("a@example.com");

        ResponseEntity<?> res = new UserController().getMyInfo(member);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ((Map<String, Object>) res.getBody()).get("data");
        assertThat(data).containsEntry("id", 1L).containsEntry("email", "a@example.com")
                .containsEntry("name", null).containsEntry("picture", null);
    }
}
