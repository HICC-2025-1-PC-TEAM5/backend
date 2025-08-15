package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.dto.*;
import hicc_project.RottenToday.dto.RecipeRequestDto;
import hicc_project.RottenToday.dto.TasteRecipeListResponse;
import hicc_project.RottenToday.entity.Member;
import hicc_project.RottenToday.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UserController {
    @Autowired
    private UserService userService;



    @GetMapping("/api/users/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal Member member) {
        System.out.println(3);
        System.out.println("member = "+ member);
        if (member == null) return ResponseEntity.status(401).body(Map.of("error","Unauthorized"));
        var body = Map.of(
                "id", member.getId(),
                "email", member.getEmail(),
                "name", member.getName(),
                "picture", member.getPicture()
        );
        System.out.println(body);
        return ResponseEntity.ok(Map.of("data", body));
    }

    @DeleteMapping("/api/users/me")
    public ResponseEntity<String> deleteMyAccount(@AuthenticationPrincipal Member member) {
        userService.deleteMember(member.getEmail()); // 또는 member.getId() 방식도 가능
        return ResponseEntity.ok("회원 탈퇴 완료");
    }

    @GetMapping("/api/users/{userId}/preference")
    public ResponseEntity<PreferenceDto> getUserPreference(@PathVariable Long userId) {
        PreferenceDto response = userService.getTaste(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/users/{userId}/preference")
    public ResponseEntity<String> updateUserPreference(@PathVariable Long userId, @RequestBody RecipeRequestDto requestDto) {
        userService.updateTaste(userId, requestDto.getRecipeId(), requestDto.getType());
        return ResponseEntity.ok("ok");
    }

    @DeleteMapping("/api/users/{userId}/preference")
    public ResponseEntity<String> deleteUserPreference(@PathVariable Long userId, @RequestBody RecipeRequestDto requestDto) {
        userService.deleteTaste(requestDto.getRecipeId(), userId);
        return ResponseEntity.ok("ok");

    }

    @GetMapping("/api/users/{userId}/preference/allergy")
    public ResponseEntity<AllergyListResponse> getAllergy(@PathVariable Long userId) {
        AllergyListResponse response = userService.getAllergy(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/users/{userId}/preference/allergy")
    public ResponseEntity<String> addAllergy(@PathVariable Long userId, @RequestBody IngredientDto request) {
        userService.addAllergy(userId, request);
        return ResponseEntity.ok("ok");
    }

    @DeleteMapping("/api/users/{userId}/preference/allergy/{allergyId}")
    public ResponseEntity<String> deleteAllergy(@PathVariable Long userId, @PathVariable Long allergyId) {
        userService.deleteAllergy(allergyId);
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/api/users/{userId}/history")
    public ResponseEntity<HistoryListResponse> getHistory(@PathVariable Long userId) {
        HistoryListResponse history = userService.getHistory(userId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/api/users/{userId}/history")
    public ResponseEntity<HistoryDto> updateHistory(@PathVariable Long userId, @RequestBody RecipeRequestDto requestDto) {
        HistoryDto historyDto = userService.updateHistory(userId, requestDto.getRecipeId());
        return ResponseEntity.ok(historyDto);
    }

    @GetMapping("/api/users/{userId}/history/favorites")
    public ResponseEntity<HistoryListResponse> getFavorites(@PathVariable Long userId) {
        HistoryListResponse favorites = userService.getFavorites(userId);
        return ResponseEntity.ok(favorites);
    }

    @PostMapping("/api/users/{userId}/history/favorites")
    public ResponseEntity<String> updateFavorite(@PathVariable Long userId, @RequestBody FavoriteRequestDto requestDto) {
        userService.updateFavorites(userId, requestDto);
        return ResponseEntity.ok("ok");
    }



}
