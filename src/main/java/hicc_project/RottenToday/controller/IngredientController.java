package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.dto.RefridgeIngredientRequest;
import hicc_project.RottenToday.dto.RefrigeratorIngredientResponse;
import hicc_project.RottenToday.entity.RefrigeratorIngredient;
import hicc_project.RottenToday.service.IngredientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class IngredientController {

    private final IngredientService ingredientService;

    @Autowired
    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @PostMapping("/api/users/fridge/receipt-to-ingredients")
    public ResponseEntity<List<List<String>>> receiptToIngredients(@RequestParam MultipartFile image) throws IOException, InterruptedException {
        List<List<String>> response = ingredientService.detectIngredient(image);
        return ResponseEntity.ok(response);
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

    @DeleteMapping("/api/users/{userId}/frigge/ingredients")
    public ResponseEntity<String> deleteRefrigeratorIngredient(@PathVariable Long userId, @RequestBody RefridgeIngredientRequest request) {
        ingredientService.deleteIngredient(userId, request);
        return ResponseEntity.ok("ok");

    }
}