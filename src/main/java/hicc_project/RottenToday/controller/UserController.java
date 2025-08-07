package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.dto.RecipeRequestDto;
import hicc_project.RottenToday.dto.TasteRecipeListResponse;
import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<Member> getMyInfo(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(member);
    }

    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMyAccount(@AuthenticationPrincipal Member member) {
        userService.deleteMember(member.getEmail()); // 또는 member.getId() 방식도 가능
        return ResponseEntity.ok("회원 탈퇴 완료");
    }

    @GetMapping("/{userId}/preference")
    public ResponseEntity<TasteRecipeListResponse> getUserPreference(@PathVariable Long userId) {
        TasteRecipeListResponse tasteRecipeListResponse = userService.getTaste(userId);
        return ResponseEntity.ok(tasteRecipeListResponse);
    }

    @PostMapping("/{userId}/preference")
    public ResponseEntity<String> updateUserPreference(@PathVariable Long userId, @RequestBody RecipeRequestDto requestDto) {
        userService.updateTaste(requestDto.getRecipeId(), requestDto.getType());
        return ResponseEntity.ok("ok");
    }

    @DeleteMapping("/{userId}/preference")
    public ResponseEntity<String> deleteUserPreference(@PathVariable Long userId, @RequestBody RecipeRequestDto requestDto) {
        userService.deleteTaste(requestDto.getRecipeId());
        return ResponseEntity.ok("ok");
    }
}
