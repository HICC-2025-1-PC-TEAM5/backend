package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.dto.RecipeRequestDto;
import hicc_project.RottenToday.dto.TasteRecipeListResponse;
import hicc_project.RottenToday.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/users/{userId}/preference")
    public ResponseEntity<TasteRecipeListResponse> getUserPreference(@PathVariable Long userId) {
        TasteRecipeListResponse tasteRecipeListResponse = userService.getTaste(userId);
        return ResponseEntity.ok(tasteRecipeListResponse);
    }

    @PostMapping("/api/users/{userId}/preference")
    public ResponseEntity<String> updateUserPreference(@PathVariable Long userId, @RequestBody RecipeRequestDto requestDto) {
        userService.updateTaste(requestDto.getRecipeId(), requestDto.getType());
        return ResponseEntity.ok("ok");
    }

    @DeleteMapping("/api/users/{userId}/preference")
    public ResponseEntity<String> deleteUserPreference(@PathVariable Long userId, @RequestBody RecipeRequestDto requestDto) {
        userService.deleteTaste(requestDto.getRecipeId());
        return ResponseEntity.ok("ok");

    }

}
