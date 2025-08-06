package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.service.IngredientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class IngredientController {

    private final IngredientService ingredientService;

    @Autowired
    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @PostMapping("/api/users/fridge/receipt-to-ingredients")
    public ResponseEntity<String> receiptToIngredients(@RequestParam MultipartFile image) throws IOException, InterruptedException {
        String response = ingredientService.detectIngredient(image);
        return ResponseEntity.ok(response);
    }

}