package hicc_project.RottenToday.dto;

import hicc_project.RottenToday.entity.RecipeStep;

/** 조리 단계 응답 (엔티티를 그대로 내보내지 않는다). 필드는 이전 엔티티 직렬화 결과와 같다 */
public record RecipeStepDto(Long id, int stepNum, String description, String image) {

    public static RecipeStepDto from(RecipeStep step) {
        return new RecipeStepDto(step.getId(), step.getStepNum(), step.getDescription(), step.getImage());
    }

    public static java.util.List<RecipeStepDto> fromAll(java.util.List<RecipeStep> steps) {
        return steps == null ? java.util.List.of() : steps.stream().map(RecipeStepDto::from).toList();
    }
}
