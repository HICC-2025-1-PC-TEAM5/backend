package hicc_project.RottenToday.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PreferenceDto {
    private List<LikeDto> like;
    private List<DislikeDto> dislike;

    public PreferenceDto(List<LikeDto> like, List<DislikeDto> dislike) {
        this.like = like;
        this.dislike = dislike;
    }
}
