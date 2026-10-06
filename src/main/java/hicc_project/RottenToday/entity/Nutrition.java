package hicc_project.RottenToday.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

// 재료 마스터의 대표 영양성분 (D-013). 출처: 전국통합식품영양성분정보 원재료성식품 표준데이터
// 값이 없으면 null (CSV에 측정값이 없거나 직접 추가한 재료)
// 컬럼 이름은 명시한다: 기본 규칙은 끝의 대문자 한 글자 앞에 '_'를 넣지 않아 carbohydrateG → carbohydrateg가 된다
@Data
@Embeddable
public class Nutrition {
    @Column(name = "nutrient_basis")
    private String nutrientBasis;      // 영양성분 함량 기준량 (예: 100g)
    @Column(name = "energy_kcal")
    private Double energyKcal;         // 에너지(kcal)
    @Column(name = "carbohydrate_g")
    private Double carbohydrateG;      // 탄수화물(g)
    @Column(name = "protein_g")
    private Double proteinG;           // 단백질(g)
    @Column(name = "fat_g")
    private Double fatG;               // 지방(g)
    @Column(name = "sugar_g")
    private Double sugarG;             // 당류(g)
    @Column(name = "dietary_fiber_g")
    private Double dietaryFiberG;      // 식이섬유(g)
    @Column(name = "sodium_mg")
    private Double sodiumMg;           // 나트륨(mg)
    @Column(name = "source_food_code", length = 50)
    private String sourceFoodCode;     // 대표값을 가져온 CSV 행의 식품코드
}
