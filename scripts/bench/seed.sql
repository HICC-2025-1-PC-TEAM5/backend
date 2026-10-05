-- 측정용 회원과 알레르기 재료. 앱을 한 번 기동해 테이블이 생긴 뒤에 실행한다.
INSERT INTO member (email, name, provider, external_id, token_version)
VALUES ('bench@cookit.test', 'bench', 'bench', 'bench-0001', 0);

-- 알레르기 필터 확인용 (Category는 ORDINAL로 저장, 7 = BEANS 두류/콩류)
INSERT INTO ingredient (name, category) VALUES ('땅콩', 7);
INSERT INTO allergy (member_id, ingredient_id)
SELECT m.id, i.id FROM member m, ingredient i WHERE m.external_id = 'bench-0001' AND i.name = '땅콩';

SELECT id AS bench_member_id FROM member WHERE external_id = 'bench-0001';
