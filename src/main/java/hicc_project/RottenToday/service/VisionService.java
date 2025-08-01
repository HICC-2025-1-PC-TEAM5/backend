package hicc_project.RottenToday.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import hicc_project.RottenToday.dto.ImageToIngredientResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class VisionService {
    public ImageToIngredientResponse detectIngredient(byte[] imageBytes) throws IOException {
        List<String> labels = new ArrayList<>();

        // Detects labels in the specified local image.
        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
            ByteString imgBytes = ByteString.copyFrom(imageBytes);
            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
            AnnotateImageRequest request =
                    AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();

            List<AnnotateImageRequest> requests = new ArrayList<>();
            requests.add(request);

            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);

            for (AnnotateImageResponse res : response.getResponsesList()) {
                if (res.hasError()) {
                    throw new IOException("Vision API ERROR: " + res.getError().getMessage());
                }
                for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
                    labels.add(annotation.getDescription());
                }
            }
        }

        return new ImageToIngredientResponse(labels);
    }
}
