-- 레시피 로컬 DB 적재 전 초기화 (D-022 결정 4-A). 실행 전에 반드시 백업한다:
--   docker compose exec -T mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --default-character-set=utf8mb4 "$MYSQL_DATABASE"' > backups/<이름>.sql
-- 요청마다 저장하던 레시피와 이를 참조하는 기록·취향(로컬 테스트 데이터)을 지운다. 회원·냉장고·알레르기·재료 마스터는 그대로 둔다
DELETE FROM history;
DELETE FROM taste;
DELETE FROM recipe_ingredient;
DELETE FROM recipe_step;
DELETE FROM recipe;
