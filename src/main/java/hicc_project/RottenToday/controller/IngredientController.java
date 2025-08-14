package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.dto.*;
import hicc_project.RottenToday.entity.RefrigeratorIngredient;
import hicc_project.RottenToday.service.IngredientService;
import hicc_project.RottenToday.service.OpenAiService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class IngredientController {

    private final IngredientService ingredientService;
    private final OpenAiService openAiService;

    @Autowired
    public IngredientController(IngredientService ingredientService, OpenAiService openAiService) {
        this.ingredientService = ingredientService;
        this.openAiService = openAiService;
    }

    @PostMapping("/api/users/{userId}/fridge/receipt-to-ingredients")
    public ResponseEntity<List<IngredientDto>> receiptToIngredients(@RequestParam MultipartFile image) throws IOException, InterruptedException {
        List<List<String>> response = ingredientService.detectIngredient(image);
        List<IngredientDto> chatCompletion = openAiService.getChatCompletion(response.toString());
        return ResponseEntity.ok(chatCompletion);
    }

    @GetMapping("/api/users/{userId}/fridge/ingredients")
    public ResponseEntity<RefrigeratorIngredientResponse> getRefrigeratorIngredient(@PathVariable Long userId) {
        RefrigeratorIngredientResponse response = ingredientService.getRefidge(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/users/{userId}/fridge/ingredients")
    public ResponseEntity<String> addRefrigeratorIngredient(@PathVariable Long userId, @RequestBody RefrigeratorIngredientResponse request) {
        ingredientService.addRefridgeIngredient(userId, request);
        return ResponseEntity.ok("ok");
    }

    @PatchMapping("/api/users/{userId}/fridge/ingredients")
    public ResponseEntity<String> updateRefrigeratorIngredient(@PathVariable Long userId, @RequestBody RefridgeIngredientRequest request) {
        ingredientService.updateRefridgeIngredient(userId, request);
        return ResponseEntity.ok("ok");
    }

    @DeleteMapping("/api/users/{userId}/fridge/ingredients/{refrigeratorId}")
    public ResponseEntity<String> deleteRefrigeratorIngredient(@PathVariable Long userId, @PathVariable Long refrigeratorId) {
        ingredientService.deleteIngredient(userId, refrigeratorId);
        return ResponseEntity.ok("ok");

    }

    @GetMapping("/api/users/{userId}/ingredientlist/{ingredientId}")
    public ResponseEntity<IngredientResponseDto> getIngredientDetail(@PathVariable Long userId, @PathVariable Long ingredientId) {
        IngredientResponseDto ingredientDetail = ingredientService.getIngredientDetail(userId, ingredientId);
        return ResponseEntity.ok(ingredientDetail);
    }

    @GetMapping("/api/users/{userId}/ingredientlist")
    public ResponseEntity<IngredientListResponse> getIngredientList(@PathVariable Long userId) {
        IngredientListResponse ingredientList = ingredientService.getIngredientList(userId);
        return ResponseEntity.ok(ingredientList);
    }





}