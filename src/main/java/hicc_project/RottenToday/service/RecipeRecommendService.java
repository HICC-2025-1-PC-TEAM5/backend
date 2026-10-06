package hicc_project.RottenToday.service;

import hicc_project.RottenToday.dto.RecipeResponseDto;
import hicc_project.RottenToday.dto.RefridgeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// 냉장고 재료로 레시피를 추천한다 (C1, D-018)
// 재료는 서버가 사용자 냉장고에서 고른다. 레시피 출처를 로컬 DB로 바꾸면(레시피 계획 Phase 5) 이 서비스 안쪽만 바꾼다
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeRecommendService {

    private final IngredientService ingredientService;
    private final RecipeService recipeService;

    public List<RecipeResponseDto> recommendFromFridge(Long memberId) {
        List<RefridgeDto> fridge = ingredientService.getRefidge(memberId).getRefrigeratorIngredient();
        Optional<String> ingredient = pickIngredient(fridge);
        if (ingredient.isEmpty()) {
            // 냉장고가 비어 있으면 외부 API를 부르지 않는다
            return List.of();
        }
        return recipeService.getRecipeByIngredients(List.of(ingredient.get()), memberId);
    }

    /**
     * 소비기한이 가장 가까운 재료 1개 (소비기한이 없으면 뒤로, 같으면 먼저 넣은 재료).
     * 식품안전나라 COOKRCP01은 RCP_PARTS_DTLS를 여러 개 보내도 마지막 값만 쓰므로 1개만 보낸다
     */
    static Optional<String> pickIngredient(List<RefridgeDto> fridge) {
        if (fridge == null) return Optional.empty();
        Comparator<LocalDateTime> nullsLast = Comparator.nullsLast(Comparator.naturalOrder());
        return fridge.stream()
                .filter(it -> StringUtils.hasText(it.getName()))
                .min(Comparator.comparing(RefridgeDto::getExpire_date, nullsLast)
                        .thenComparing(RefridgeDto::getInput_date, nullsLast))
                .map(it -> it.getName().trim());
    }
}
