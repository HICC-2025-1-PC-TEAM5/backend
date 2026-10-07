-- V2: 레시피 로컬 DB 적재 준비 (레시피 계획 Phase 2, D-020·D-023·D-025)

-- 식품안전나라 레시피 일련번호. 같은 레시피를 두 번 적재하지 않도록 unique로 둔다
-- 기존 행(요청마다 저장한 레시피)은 NULL이며, MySQL unique는 NULL을 여러 개 허용한다
ALTER TABLE recipe
    ADD COLUMN rcp_seq VARCHAR(20) NULL,
    ADD CONSTRAINT uk_recipe_rcp_seq UNIQUE (rcp_seq),
    MODIFY ingredients TEXT;

-- 단계 설명이 255자를 넘어도 적재가 실패하지 않게 한다 (2026-10-08 CSV 최대 162자)
ALTER TABLE recipe_step
    MODIFY description TEXT;

-- 레시피 재료: 정규화한 이름(추천 매칭에 쓴다), 원문 조각, 마스터에 없어 확인이 필요한지 (D-025)
ALTER TABLE recipe_ingredient
    ADD COLUMN name VARCHAR(255) NULL,
    ADD COLUMN raw_text VARCHAR(500) NULL,
    ADD COLUMN needs_review BIT(1) NOT NULL DEFAULT b'0',
    ADD INDEX idx_recipe_ingredient_name (name);
