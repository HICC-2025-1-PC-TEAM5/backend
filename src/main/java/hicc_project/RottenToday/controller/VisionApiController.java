package hicc_project.RottenToday.controller;

import hicc_project.RottenToday.dto.ImageToIngredientResponse;
import hicc_project.RottenToday.dto.IngredientDto;
import hicc_project.RottenToday.service.OpenAiService;
import hicc_project.RottenToday.service.S3UploadService;
import hicc_project.RottenToday.service.VisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
public class VisionApiController {
    private final VisionService visionService;
    private OpenAiService openAiService;
    private S3UploadService s3UploadService;
    @Autowired
    public VisionApiController(VisionService visionService, OpenAiService openAiService, S3UploadService s3UploadService) {
        this.visionService = visionService;
        this.openAiService = openAiService;
        this.s3UploadService = s3UploadService;
    }

    @PostMapping("/api/users/{userId}/fridge/image-to-ingredients")
    public ResponseEntity<List<IngredientDto>> analyzeIngredients(@RequestParam("image") MultipartFile image) throws IOException {
        System.out.println(123123);
        byte[] imageBytes =image.getBytes();
        ImageToIngredientResponse labels = visionService.detectIngredient(imageBytes);
        //String url = s3UploadService.saveFile(image);
        //System.out.println(url);
        //List<IngredientDto> getimagetoingredient = openAiService.getimagetoingredient(url);
        List<IngredientDto> getimagetoingredient = openAiService.getpicturetoingredient(labels.getIngredient());

        return ResponseEntity.ok(getimagetoingredient);

    }
}
