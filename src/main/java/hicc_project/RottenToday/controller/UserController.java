package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.dto.*;
import hicc_project.RottenToday.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {
    @Autowired
    private UserService userService;


    @GetMapping("/api/users/{userId}/preference")
    public ResponseEntity<TasteRecipeListResponse> getUserPreference(@PathVariable Long userId) {
        TasteRecipeListResponse tasteRecipeListResponse = userService.getTaste(userId);
        return ResponseEntity.ok(tasteRecipeListResponse);
    }

    @PostMapping("/api/users/{userId}/preference")
    public ResponseEntity<String> updateUserPreference(@PathVariable Long userId, @RequestBody RecipeRequestDto requestDto) {
        userService.updateTaste(userId, requestDto.getRecipeId(), requestDto.getType());
        return ResponseEntity.ok("ok");
    }

    @DeleteMapping("/api/users/{userId}/preference")
    public ResponseEntity<String> deleteUserPreference(@PathVariable Long userId, @RequestBody RecipeRequestDto requestDto) {
        userService.deleteTaste(requestDto.getRecipeId());
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
    public ResponseEntity<String> updateHistory(@PathVariable Long userId, @RequestBody RecipeRequestDto requestDto) {
        userService.updateHistory(userId, requestDto.getRecipeId());
        return ResponseEntity.ok("ok");
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
