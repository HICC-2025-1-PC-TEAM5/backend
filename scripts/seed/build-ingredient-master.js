#!/usr/bin/env node
// 재료 마스터 seed SQL 생성 (D-013)
// 입력: 공공데이터 "전국통합식품영양성분정보_원재료성식품_표준데이터.csv" (CP949)
// 사용 (backend/에서): node scripts/seed/build-ingredient-master.js <csv 경로> > scripts/seed/ingredient_master.sql
'use strict';
const fs = require('fs');

const csvPath = process.argv[2];
if (!csvPath) {
  console.error('사용법: node scripts/seed/build-ingredient-master.js <csv 경로>');
  process.exit(1);
}

// Category enum 순서(ORDINAL 저장)와 같아야 한다: entity/Category
const CATEGORY = {
  VEGETABLE: 0, FRUIT: 1, GRAIN: 2, MEAT: 3, SEAFOOD: 4, EGG: 5, DAIRY: 6,
  BEANS: 7, OIL: 8, CONDIMENT: 9, PROCESSED: 10, DRINK: 11, ETC: 12,
};

// 식품대분류명 → Category (D-013)
const CATEGORY_MAP = {
  '채소류': CATEGORY.VEGETABLE,
  '버섯류': CATEGORY.VEGETABLE,
  '과일류': CATEGORY.FRUIT,
  '곡류': CATEGORY.GRAIN,
  '감자 및 전분류': CATEGORY.GRAIN,
  '육류': CATEGORY.MEAT,
  '어패류 및 기타 수산물': CATEGORY.SEAFOOD,
  '해조류': CATEGORY.SEAFOOD,
  '난류': CATEGORY.EGG,
  '우유류': CATEGORY.DAIRY,
  '두류': CATEGORY.BEANS,
  '견과 및 종실류': CATEGORY.BEANS,
  '유지류': CATEGORY.OIL,
  '조미료류': CATEGORY.CONDIMENT,
  '당류': CATEGORY.CONDIMENT,
  '차류': CATEGORY.DRINK,
  '기타': CATEGORY.ETC,
};

// 일반적으로 쓰는 이름으로 바꾼다 (D-014). 같은 이름이 되면 한 재료로 합친다
// - 괄호 속 이름이 더 흔한 경우, 크기·등급 구분, '참~'·'살~' 같은 정식 명칭
const ALIAS = {
  // 곡류
  '멥쌀': '쌀', '멥쌀밥': '밥', '멥쌀 국수': '쌀국수', '맵쌀 국수': '쌀국수',
  // 채소·버섯
  '파': '대파', '큰느타리버섯(새송이버섯)': '새송이버섯', '꽃양배추(콜리플라워)': '콜리플라워',
  '고려엉겅퀴(곤드레)': '곤드레', '고수(향채)': '고수', '산마늘(명이나물)': '명이나물',
  '퉁퉁마디(함초)': '함초', '퉁퉁마디환(함초환)': '함초환', '여주(고야)': '여주', '염교(락교)': '락교',
  '파프리카(착색단고추)': '파프리카', '무 절임(치킨무)': '치킨무', '방울다다기양배추': '방울양배추',
  '배암차즈기(곰보배추)': '곰보배추', '신선초(명일엽)': '신선초', '비타민채(다채)': '비타민채',
  '능이버섯(향버섯)': '능이버섯', '토스카노(잎브로콜리)': '잎브로콜리', '열대비름(아마란스)': '열대비름',
  '참죽나물(가죽나물)': '참죽나물', '엄나무(개두릅)': '엄나무순', '수리취(떡취)': '수리취',
  // 과일
  '자몽(그레이프프루트)': '자몽', '백향과(패션프루트)': '패션프루트', '백향과(패션프루트) 주스': '패션프루트 주스',
  '오렴자(카람볼라)': '스타프루트',
  // 두류·견과
  '콩(대두)': '콩', '렌즈콩(렌틸콩)': '렌틸콩', '개암(헤이즐넛)': '헤이즐넛', '쥐눈이콩(검정소립콩)': '쥐눈이콩',
  '피스타치오넛': '피스타치오', '삼씨(대마씨)': '대마씨', '작두(도두)': '작두콩', '완두': '완두콩', '잠두': '잠두콩',
  // 어패류·해조류
  '넙치(광어)': '광어', '조피볼락(우럭)': '우럭', '참담치(홍합)': '홍합', '멍게(우렁쉥이)': '멍게',
  '큰구슬우렁이(골뱅이)': '골뱅이', '주름미더덕(오만둥이)': '오만둥이', '토굴(벗굴)': '벗굴',
  '접시조개(비단조개)': '비단조개', '향어(이스라엘잉어)': '향어', '돌김(둥근돌김)': '돌김',
  '북방전복(참전복)': '전복', '관절매물고둥(보라골뱅이)': '보라골뱅이', '콩깍지고둥(털골뱅이)': '털골뱅이',
  '긴고둥(긴뿔고둥)': '긴고둥', '대두어(흑연)': '대두어',
  '갈치(24cm미만)': '갈치', '갈치(24cm이상)': '갈치',
  '기름가자미(20cm미만)': '기름가자미', '기름가자미(21-24cm)': '기름가자미', '기름가자미(25-29cm)': '기름가자미',
  '기름가자미(25cm미만)': '기름가자미', '기름가자미(30cm이상)': '기름가자미', '전갱이(치어)': '전갱이',
  '살오징어': '오징어', '참굴': '굴', '참문어': '문어', '대문어': '문어', '참가리비': '가리비', '참꼬막': '꼬막',
  '참김': '김', '참다시마': '다시마', '참홍어': '홍어', '참조기': '조기', '참소라': '소라', '참갑오징어': '갑오징어',
  '참꼴뚜기': '꼴뚜기', '참게': '민물게', '대서양연어': '연어', '먹장어': '곰장어', '붕장어': '아나고',
  // 기름·조미료·기타
  '쌀겨기름(미강유)': '현미유', '레몬그라스(시트로넬라)': '레몬그라스', '라벤다': '라벤더', '팽창제': '베이킹파우더',
  // 레시피 재료 이름과 맞춘 일반적인 이름 (레시피 계획 Phase 1, D-025). 레시피 쪽 동의어는 src/main/resources/recipe/ingredient-aliases.csv
  '숙주나물': '숙주', '무시래기': '시래기', '월계수': '월계수잎', '곤약(구약나물)': '곤약', '유채씨기름': '카놀라유',
  '고추냉이': '와사비', '육두구': '넛맥', '배 과즙': '배즙', '녹두묵': '청포묵', '배초향(방아)': '방아잎',
};


// 이전 seed가 ALIAS로 이미 바꿔 넣은 이름을 다시 바꾼다 (D-025). CSV 원래 이름이 아니라서 ALIAS에 둘 수 없다
const PRIOR_RENAMES = { '쌀밥': '밥', '미강유': '현미유' };
// CSV에 없는 기본 재료 (기본 재료 추천 GET /fridge/necessary가 이름으로 찾는다)
const EXTRA = [
  { name: '쌀', category: CATEGORY.GRAIN },
  { name: '고추장', category: CATEGORY.CONDIMENT },
  { name: '식용유', category: CATEGORY.OIL },
  // 레시피에 자주 나오지만 원재료성식품 CSV에 없는 재료 104개 (D-027, 레시피 1,156건의 미연결 상위 120개 중 별칭·중간 재료 제외)
  // 카테고리는 Claude 서브에이전트 1차 분류(기존 마스터 선례 우선). 근거: docs/reviews/2026-10-07-master-additions.md
  ...['홍고추', '애호박', '단호박', '깻잎', '청양고추', '방울토마토', '어린잎채소', '쪽파', '양상추', '실파', '건표고버섯', '새싹채소', '적양배추', '영양부추', '알배추', '적양파', '건고추', '래디시', '식용꽃', '주키니호박'].map((name) => ({ name, category: CATEGORY.VEGETABLE })),
  ...['레몬즙', '건포도', '오렌지주스', '홍시'].map((name) => ({ name, category: CATEGORY.FRUIT })),
  ...['밀가루', '찹쌀가루', '빵가루', '현미', '강력분', '박력분', '튀김가루', '떡볶이떡', '스파게티', '라이스페이퍼', '실곤약', '쌀가루', '식빵'].map((name) => ({ name, category: CATEGORY.GRAIN })),
  ...['닭가슴살', '삼겹살', '돼지등심', '닭다리살', '소고기등심'].map((name) => ({ name, category: CATEGORY.MEAT })),
  ...['건새우', '가쓰오부시', '관자'].map((name) => ({ name, category: CATEGORY.SEAFOOD })),
  ...['달걀흰자', '달걀노른자'].map((name) => ({ name, category: CATEGORY.EGG })),
  ...['버터', '생크림', '요거트', '모짜렐라치즈', '파마산치즈', '치즈', '크림치즈'].map((name) => ({ name, category: CATEGORY.DAIRY })),
  ...['두부', '검은깨', '들깻가루', '두유', '견과류', '아몬드가루', '연두부', '콩가루', '순두부', '검은콩', '땅콩버터'].map((name) => ({ name, category: CATEGORY.BEANS })),
  ...['고추기름'].map((name) => ({ name, category: CATEGORY.OIL })),
  ...['식초', '올리고당', '매실청', '청주', '맛술', '마요네즈', '유자청', '카레가루', '맛간장', '케첩', '물엿', '국간장', '발사믹식초', '생강청', '굴소스', '머스터드', '함초소금', '화이트와인', '강황가루', '겨자가루', '레드와인', '미소된장', '사과식초', '알룰로스', '바질가루', '새우젓', '진간장', '백년초가루'].map((name) => ({ name, category: CATEGORY.CONDIMENT })),
  ...['김치', '베이컨', '토마토페이스트', '라면', '토마토소스', '게맛살', '백김치', '홀토마토'].map((name) => ({ name, category: CATEGORY.PROCESSED })),
  ...['드라이이스트', '판젤라틴'].map((name) => ({ name, category: CATEGORY.ETC })),
];

const NUTRIENTS = [
  ['nutrient_basis', '영양성분함량기준량', 'text'],
  ['energy_kcal', '에너지(kcal)', 'num'],
  ['carbohydrate_g', '탄수화물(g)', 'num'],
  ['protein_g', '단백질(g)', 'num'],
  ['fat_g', '지방(g)', 'num'],
  ['sugar_g', '당류(g)', 'num'],
  ['dietary_fiber_g', '식이섬유(g)', 'num'],
  ['sodium_mg', '나트륨(mg)', 'num'],
  ['source_food_code', '식품코드', 'text'],
];

function parseCsv(text) {
  const rows = [];
  let row = [], field = '', quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"') {
        if (text[i + 1] === '"') { field += '"'; i++; } else quoted = false;
      } else field += c;
    } else if (c === '"') quoted = true;
    else if (c === ',') { row.push(field); field = ''; }
    else if (c === '\n') { row.push(field.replace(/\r$/, '')); rows.push(row); row = []; field = ''; }
    else field += c;
  }
  if (field || row.length) { row.push(field); rows.push(row); }
  return rows;
}

const text = new TextDecoder('euc-kr').decode(fs.readFileSync(csvPath));
const [header, ...body] = parseCsv(text);
const col = (name) => {
  const i = header.indexOf(name);
  if (i < 0) throw new Error(`CSV에 '${name}' 컬럼이 없습니다`);
  return i;
};
const rows = body.filter((r) => r.length === header.length);

// 이름: 대표식품명. '~류'(장어류 등)는 식품중분류명으로 푼다 (D-013)
function rawNameOf(r) {
  const rep = r[col('대표식품명')].trim();
  const mid = r[col('식품중분류명')].trim();
  if (rep.endsWith('류') && mid && mid !== '해당없음') return mid;
  return rep;
}
const nameOf = (r) => ALIAS[rawNameOf(r)] || rawNameOf(r);

// 별칭이 CSV에 없는 이름을 가리키면(오타 등) 알 수 있게 한다
const rawNames = new Set(rows.map(rawNameOf));
const unusedAlias = Object.keys(ALIAS).filter((k) => !rawNames.has(k));
if (unusedAlias.length) throw new Error(`CSV에 없는 별칭: ${unusedAlias.join(', ')}`);

const groups = new Map();
for (const r of rows) {
  const name = nameOf(r);
  if (!groups.has(name)) groups.set(name, []);
  groups.get(name).push(r);
}

// 대표 행: 세분류 '생것' + 식품명에 '대표' → '생것' → 첫 행 (D-013)
function representative(list) {
  const raw = list.filter((r) => r[col('식품세분류명')] === '생것');
  return raw.find((r) => r[col('식품명')].includes('대표')) || raw[0] || list[0];
}

// 카테고리: 그룹 안에서 가장 많은 대분류. 동률이면 먼저 나온 것
function categoryOf(list) {
  const count = new Map();
  for (const r of list) {
    const big = r[col('식품대분류명')];
    count.set(big, (count.get(big) || 0) + 1);
  }
  const [big] = [...count.entries()].sort((a, b) => b[1] - a[1])[0];
  if (!(big in CATEGORY_MAP)) throw new Error(`매핑 없는 식품대분류명: ${big}`);
  return CATEGORY_MAP[big];
}

const sqlText = (v) => (v === '' || v == null ? 'NULL' : `'${String(v).replace(/'/g, "''")}'`);
const sqlNum = (v) => (v === '' || v == null || isNaN(Number(v)) ? 'NULL' : String(Number(v)));

const values = [];
for (const [name, list] of [...groups.entries()].sort((a, b) => a[0].localeCompare(b[0], 'ko'))) {
  const rep = representative(list);
  const nutrients = NUTRIENTS.map(([, csvCol, type]) => (type === 'num' ? sqlNum : sqlText)(rep[col(csvCol)]));
  values.push(`(${sqlText(name)}, ${categoryOf(list)}, ${nutrients.join(', ')})`);
}
for (const e of EXTRA) {
  if (groups.has(e.name)) continue;
  values.push(`(${sqlText(e.name)}, ${e.category}, ${NUTRIENTS.map(() => 'NULL').join(', ')})`);
}

const cols = ['name', 'category', ...NUTRIENTS.map(([c]) => c)];
const out = [];
out.push('-- 재료 마스터 seed (D-013). 이 파일은 scripts/seed/build-ingredient-master.js가 생성한다. 직접 고치지 않는다');
out.push('-- 출처: 공공데이터포털 전국통합식품영양성분정보_원재료성식품_표준데이터 (CSV ' + rows.length + '행 → 재료 ' + groups.size + '개)');
out.push('-- 추가: CSV에 없는 기본 재료 ' + EXTRA.filter((e) => !groups.has(e.name)).length + '개 (영양성분 없음, D-013·D-027): ' + EXTRA.filter((e) => !groups.has(e.name)).map((e) => e.name).join(', '));
out.push('-- 앱을 한 번 기동해 Flyway가 ingredient 테이블을 만든 뒤 실행한다 (D-023)');
out.push('-- 여러 번 실행해도 된다: 없는 이름은 추가하고, 이미 있는 행은 카테고리·영양성분·출처를 이 seed 값으로 맞춘다(id·이미지는 유지, B27)');
out.push('');
out.push('CREATE TEMPORARY TABLE ingredient_seed (');
out.push('  name VARCHAR(255) NOT NULL, category TINYINT NOT NULL, nutrient_basis VARCHAR(255),');
out.push('  energy_kcal DOUBLE, carbohydrate_g DOUBLE, protein_g DOUBLE, fat_g DOUBLE,');
out.push('  sugar_g DOUBLE, dietary_fiber_g DOUBLE, sodium_mg DOUBLE, source_food_code VARCHAR(50)');
// ingredient 테이블(V1)과 같은 collation으로 만든다. 서버 기본값(MySQL 8 기본 utf8mb4_0900_ai_ci)을 따르면 이름 비교가 'Illegal mix of collations'로 실패한다
out.push(') DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;');
out.push('');
out.push(`INSERT INTO ingredient_seed (${cols.join(', ')}) VALUES`);
out.push(values.map((v) => '  ' + v).join(',\n') + ';');
out.push('');
out.push('-- 0) 이전 seed로 들어간 옛 이름 정리 (D-014)');
out.push('--    새 이름이 없으면 이름만 바꾸고(id 유지 → 알레르기·냉장고 연결 유지),');
out.push('--    새 이름이 이미 있으면 옛 행은 어디서도 참조하지 않을 때만 지운다');
for (const [oldName, newName] of [...Object.entries(ALIAS), ...Object.entries(PRIOR_RENAMES)]) {
  const o = sqlText(oldName), n = sqlText(newName);
  out.push(`UPDATE ingredient o LEFT JOIN ingredient n ON n.name = ${n} SET o.name = ${n} WHERE o.name = ${o} AND n.id IS NULL;`);
  out.push(`DELETE o FROM ingredient o JOIN ingredient n ON n.name = ${n} AND n.id <> o.id`
    + ' LEFT JOIN allergy a ON a.ingredient_id = o.id LEFT JOIN refrigerator_ingredient r ON r.ingredient_id = o.id'
    + ' LEFT JOIN recipe_ingredient ri ON ri.ingredient_id = o.id'
    + ` WHERE o.name = ${o} AND a.id IS NULL AND r.id IS NULL AND ri.id IS NULL;`);
}
out.push('');
out.push('-- 1) 없는 이름만 추가');
out.push(`INSERT INTO ingredient (${cols.join(', ')})`);
out.push(`SELECT ${cols.map((c) => 's.' + c).join(', ')} FROM ingredient_seed s`);
out.push('WHERE NOT EXISTS (SELECT 1 FROM ingredient i WHERE i.name = s.name);');
out.push('');
out.push('-- 2) 이미 있던 행: seed가 기준이다. 카테고리·영양성분·출처를 seed 값으로 맞춘다 (id·이미지는 그대로, B27)');
out.push('--    예전 seed로 만든 DB는 대표 행이 달라 영양성분 출처가 달랐다(해산물 10개, 파프리카는 V3)');
out.push('UPDATE ingredient i JOIN ingredient_seed s ON i.name = s.name');
out.push('SET i.category = s.category, ' + NUTRIENTS.map(([c]) => `i.${c} = s.${c}`).join(', ') + ';');
out.push('');
out.push('DROP TEMPORARY TABLE ingredient_seed;');
process.stdout.write(out.join('\n') + '\n');
