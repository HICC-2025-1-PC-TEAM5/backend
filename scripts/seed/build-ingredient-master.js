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

// CSV에 없는 기본 재료 (기본 재료 추천 GET /fridge/necessary가 이름으로 찾는다)
const EXTRA = [
  { name: '쌀', category: CATEGORY.GRAIN },
  { name: '고추장', category: CATEGORY.CONDIMENT },
  { name: '식용유', category: CATEGORY.OIL },
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
function nameOf(r) {
  const rep = r[col('대표식품명')].trim();
  const mid = r[col('식품중분류명')].trim();
  if (rep.endsWith('류') && mid && mid !== '해당없음') return mid;
  return rep;
}

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
out.push('-- 추가: CSV에 없는 기본 재료 ' + EXTRA.filter((e) => !groups.has(e.name)).map((e) => e.name).join(', ') + ' (영양성분 없음)');
out.push('-- 앱을 한 번 기동해 ingredient 테이블과 영양성분 컬럼이 생긴 뒤 실행한다 (ddl-auto=update)');
out.push('-- 여러 번 실행해도 된다: 없는 이름만 추가하고, 이미 있는 행은 영양성분이 비어 있을 때만 채운다');
out.push('');
out.push('CREATE TEMPORARY TABLE ingredient_seed (');
out.push('  name VARCHAR(255) NOT NULL, category TINYINT NOT NULL, nutrient_basis VARCHAR(255),');
out.push('  energy_kcal DOUBLE, carbohydrate_g DOUBLE, protein_g DOUBLE, fat_g DOUBLE,');
out.push('  sugar_g DOUBLE, dietary_fiber_g DOUBLE, sodium_mg DOUBLE, source_food_code VARCHAR(50)');
out.push(');');
out.push('');
out.push(`INSERT INTO ingredient_seed (${cols.join(', ')}) VALUES`);
out.push(values.map((v) => '  ' + v).join(',\n') + ';');
out.push('');
out.push('-- 1) 없는 이름만 추가');
out.push(`INSERT INTO ingredient (${cols.join(', ')})`);
out.push(`SELECT ${cols.map((c) => 's.' + c).join(', ')} FROM ingredient_seed s`);
out.push('WHERE NOT EXISTS (SELECT 1 FROM ingredient i WHERE i.name = s.name);');
out.push('');
out.push('-- 2) 이미 있던 행: 카테고리·이미지는 그대로 두고 영양성분이 비어 있으면 채운다');
out.push('UPDATE ingredient i JOIN ingredient_seed s ON i.name = s.name');
out.push('SET ' + NUTRIENTS.map(([c]) => `i.${c} = s.${c}`).join(', '));
out.push('WHERE i.source_food_code IS NULL AND s.source_food_code IS NOT NULL;');
out.push('');
out.push('DROP TEMPORARY TABLE ingredient_seed;');
process.stdout.write(out.join('\n') + '\n');
