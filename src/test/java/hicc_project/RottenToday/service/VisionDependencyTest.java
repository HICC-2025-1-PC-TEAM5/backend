package hicc_project.RottenToday.service;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// B21: 다른 의존성이 protobuf 버전을 바꾸면 Vision 요청 객체를 만들 때 NoClassDefFoundError가 난다 (외부 호출 없음)
class VisionDependencyTest {

    @Test
    void Vision_요청_객체를_만들_수_있다() {
        Feature feat = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
        Image img = Image.newBuilder().setContent(ByteString.copyFrom(new byte[]{1, 2, 3})).build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();

        assertThat(request.getFeatures(0).getType()).isEqualTo(Feature.Type.LABEL_DETECTION);
    }
}
