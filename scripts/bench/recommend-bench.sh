#!/usr/bin/env bash
# 레시피 추천 응답 시간·계측 로그 측정
# 사용 예 (backend/에서): LABEL=before USER_ID=1 N=30 bash scripts/bench/recommend-bench.sh
# 추천은 GET이고 재료는 서버가 냉장고에서 소비기한이 가장 가까운 1개를 고른다 (C1, D-018).
# 그래서 세트마다 측정 회원의 냉장고를 SQL로 바꾼다 (기존 냉장고 재료는 지워진다. 측정용 회원으로 실행)
set -uo pipefail

BASE="${BASE:-http://localhost:18080}"
USER_ID="${USER_ID:?USER_ID 필요}"
TOKEN="${TOKEN:-$(bash scripts/bench/dev-token.sh "$USER_ID")}"   # D-012: /api/users/** 로그인 필수
N="${N:-30}"; WARMUP="${WARMUP:-5}"
LABEL="${LABEL:-before}"
OUT="${OUT:-../docs/measurements/raw/$(date +%Y%m%d-%H%M)-$LABEL}"
mkdir -p "$OUT"

# type=1(냉장실), category=12(기타): API로 등록할 때처럼 비어 있지 않게 넣는다 (null이면 목록 조회가 실패)
# 첫 번째 재료의 소비기한이 가장 가깝다. 베이스라인(POST)에서 외부 API가 실제로 쓴 마지막 재료와 같게 맞췄다
declare -A FRIDGE=(
  [s1]='감자'
  [s2]='양파 두부'
  [s3]='마늘 닭고기 대파'
)
# Windows에서는 한글을 명령 인자로 넘기면 코드페이지 변환으로 깨진다 → UTF-8 SQL 파일로 넣는다
set_fridge() {
  local sql="$OUT/$1.fridge.sql" d=1
  echo "DELETE FROM refrigerator_ingredient WHERE member_id = $USER_ID;" > "$sql"
  for name in ${FRIDGE[$1]}; do
    echo "INSERT INTO refrigerator_ingredient (name, quantity, unit, type, category, input_date, expire_date, member_id) VALUES ('$name', 1, '개', 1, 12, NOW(6), NOW(6) + INTERVAL $d DAY, $USER_ID);" >> "$sql"
    d=$((d + 1))
  done
  docker compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < "$sql" 2>/dev/null
}

call() {
  curl -s -o "$OUT/last.json" -m 150 -w '%{http_code}\t%{time_total}\n' \
    -H "Authorization: Bearer $TOKEN" \
    "$BASE/api/users/$USER_ID/recipes"
}

: > "$OUT/summary.txt"
for s in s1 s2 s3; do
  set_fridge "$s"
  f="$OUT/$s.tsv"; : > "$f"
  for i in $(seq 1 "$WARMUP"); do call > /dev/null; done
  sleep 1; since=$(date -u +%Y-%m-%dT%H:%M:%SZ); sleep 1   # 워밍업 로그를 집계에서 뺀다
  for i in $(seq 1 "$N"); do
    printf '%s\t%s\n' "$i" "$(call)" >> "$f"
  done
  non200=$(awk -F'\t' '$2 != 200' "$f" | wc -l)
  cut -f3 "$f" | sort -n | awk -v s="$s" -v e="$non200" '
    { a[NR] = $1 * 1000; sum += $1 * 1000 }
    END { p = int((NR * 95 + 99) / 100);
          printf "%s n=%d avg=%.1fms p95=%.1fms max=%.1fms non200=%d\n", s, NR, sum/NR, a[p], a[NR], e }' \
    | tee -a "$OUT/summary.txt"
  docker compose logs app --no-log-prefix --since "$since" | grep 'recipe.metrics' > "$OUT/$s.metrics.log"
  awk -v s="$s" '
    { for (i = 1; i <= NF; i++) { split($i, kv, "="); v[kv[1]] = kv[2] }
      n++; ext += v["externalMs"]; svc += v["serviceMs"]; calls += v["externalCalls"]; ret += v["returned"]; sav += v["saved"] }
    END { if (n) printf "%s metrics requests=%d externalCalls/req=%.2f externalMs.avg=%.1f serverMs.avg=%.1f returned.avg=%.1f saved.avg=%.1f\n",
                        s, n, calls/n, ext/n, (svc-ext)/n, ret/n, sav/n }' "$OUT/$s.metrics.log" \
    | tee -a "$OUT/summary.txt"
done
echo "결과: $OUT"
