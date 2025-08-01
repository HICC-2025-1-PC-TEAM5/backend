package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.dto.ImageToIngredientResponse;
import hicc_project.RottenToday.service.VisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class VisionApiController {
    private final VisionService visionService;

    @Autowired
    public VisionApiController(VisionService visionService) {
        this.visionService = visionService;
    }

    @PostMapping("/api/users/fridge/image-to-ingredients")
    public ResponseEntity<ImageToIngredientResponse> analyzeIngredients(@RequestParam("image") MultipartFile image) throws IOException {
        byte[] imageBytes =image.getBytes();
        ImageToIngredientResponse labels = visionService.detectIngredient(imageBytes);
        return ResponseEntity.ok(labels);

    }
}
