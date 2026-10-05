#!/usr/bin/env bash
# 레시피 추천 응답 시간·계측 로그 측정
# 사용 예 (backend/에서): LABEL=before USER_ID=1 N=30 bash scripts/bench/recommend-bench.sh
set -uo pipefail

BASE="${BASE:-http://localhost:18080}"
USER_ID="${USER_ID:?USER_ID 필요}"
N="${N:-30}"; WARMUP="${WARMUP:-5}"
LABEL="${LABEL:-before}"
OUT="${OUT:-../docs/measurements/raw/$(date +%Y%m%d-%H%M)-$LABEL}"
mkdir -p "$OUT"

declare -A BODY=(
  [s1]='["감자"]'
  [s2]='["두부","양파"]'
  [s3]='["닭고기","대파","마늘"]'
)
# Windows에서는 한글을 curl 인자로 넘기면 코드페이지 변환으로 깨진다 → UTF-8 파일로 보낸다
for s in "${!BODY[@]}"; do printf '%s' "${BODY[$s]}" > "$OUT/$s.body.json"; done

call() {
  curl -s -o "$OUT/last.json" -m 150 -w '%{http_code}\t%{time_total}\n' \
    -X POST -H 'Content-Type: application/json; charset=utf-8' --data-binary "@$OUT/$1.body.json" \
    "$BASE/api/users/$USER_ID/recipes"
}

: > "$OUT/summary.txt"
for s in s1 s2 s3; do
  f="$OUT/$s.tsv"; : > "$f"
  for i in $(seq 1 "$WARMUP"); do call "$s" > /dev/null; done
  sleep 1; since=$(date -u +%Y-%m-%dT%H:%M:%SZ); sleep 1   # 워밍업 로그를 집계에서 뺀다
  for i in $(seq 1 "$N"); do
    printf '%s\t%s\n' "$i" "$(call "$s")" >> "$f"
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
