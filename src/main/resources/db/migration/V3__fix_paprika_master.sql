-- V3: 재료 마스터 '파프리카'를 향신료(파프리카 분말)에서 채소로 바로잡는다 (2026-10-08)
-- 예전 seed로 만든 DB는 '파프리카'가 원재료성식품 CSV의 분말 행(R118-039100006-0000, 조미료/향신료, 520kcal)으로 들어가 있다.
-- 지금 seed(build-ingredient-master.js)는 생 파프리카 행(R106-194008101-0000, 채소류, 26kcal)을 쓰지만,
-- seed는 이미 있는 이름을 건너뛰므로 기존 DB는 저절로 고쳐지지 않는다. 냉장 보관 소비기한이 365일(조미료)로 계산되던 문제
-- 분말 행일 때만 고친다. 빈 DB(seed 전)나 이미 바른 DB에서는 아무것도 바뀌지 않는다
UPDATE ingredient
SET category = 0,
    nutrient_basis = '100g',
    energy_kcal = 26,
    carbohydrate_g = 6.42,
    protein_g = 0.91,
    fat_g = 0.13,
    sugar_g = 2.65,
    dietary_fiber_g = 1.6,
    sodium_mg = 0,
    source_food_code = 'R106-194008101-0000'
WHERE name = '파프리카'
  AND source_food_code = 'R118-039100006-0000';
