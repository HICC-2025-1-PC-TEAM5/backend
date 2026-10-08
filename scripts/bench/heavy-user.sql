-- 성능 측정용 '데이터가 많은 회원' (P-014). 측정 전용 DB에서만 실행한다 (개발 DB에 넣지 않는다)
-- 전제: Flyway 스키마 + 재료 마스터 seed + 레시피 CSV 적재가 끝난 DB. 냉장고 50, 기록 100(즐겨찾기 20), 좋아요 50, 알레르기 5
INSERT IGNORE INTO member (token_version, email, external_id, name, provider) VALUES (0, 'bench-heavy@test', 'bench-heavy', '측정용', 'google');
SET @m = (SELECT id FROM member WHERE external_id = 'bench-heavy');
-- 냉장고 50개: 레시피에 많이 나오는 재료 중 마스터에 연결된 것 (양념 제외), 소비기한은 -2~+28일로 퍼뜨림
INSERT INTO refrigerator_ingredient (member_id, ingredient_id, name, quantity, unit, type, category, input_date, expire_date)
SELECT @m, t.ingredient_id, t.name, 1, '개', 1, i.category, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL (CAST(MOD(t.rn * 7, 31) AS SIGNED) - 2) DAY
FROM (SELECT ri.ingredient_id, ri.name, ROW_NUMBER() OVER (ORDER BY COUNT(*) DESC) rn
      FROM recipe_ingredient ri WHERE ri.ingredient_id IS NOT NULL
        AND ri.name NOT IN ('소금','후추','설탕','간장','식초','참기름','식용유','올리브유','참깨','깨소금','고춧가루','물엿','올리고당','맛술','청주','물')
      GROUP BY ri.ingredient_id, ri.name) t JOIN ingredient i ON i.id = t.ingredient_id
WHERE t.rn <= 50;
-- 조회 기록 100건 (서로 다른 레시피), 앞 20건은 즐겨찾기
INSERT INTO history (member_id, recipe_id, view_at, favorite)
SELECT @m, r.id, NOW() - INTERVAL CAST(r.rn AS SIGNED) HOUR, r.rn <= 20 FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) rn FROM recipe) r WHERE r.rn <= 100;
-- 좋아요 50건
INSERT INTO taste (member_id, recipe_id, type)
SELECT @m, r.id, 0 FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id DESC) rn FROM recipe) r WHERE r.rn <= 50;
-- 알레르기 5개 (레시피에 거의 안 나오는 마스터 재료)
INSERT INTO allergy (member_id, ingredient_id)
SELECT @m, i.id FROM ingredient i WHERE i.name IN ('땅콩','메밀','호두','복숭아','키위');
SELECT @m member_id, (SELECT COUNT(*) FROM refrigerator_ingredient WHERE member_id=@m) fridge, (SELECT COUNT(*) FROM history WHERE member_id=@m) history,
  (SELECT SUM(favorite) FROM history WHERE member_id=@m) favorites, (SELECT COUNT(*) FROM taste WHERE member_id=@m) taste, (SELECT COUNT(*) FROM allergy WHERE member_id=@m) allergy;
