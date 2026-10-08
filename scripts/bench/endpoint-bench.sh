#!/usr/bin/env bash
# 한 API의 응답 시간·크기 측정 (데이터를 바꾸지 않는다, GET 전용)
# 사용 (backend/에서): TOKEN=$(bash scripts/bench/dev-token.sh 1) N=50 bash scripts/bench/endpoint-bench.sh /api/users/1/recipes
# - WARMUP번 먼저 호출해 JIT·커넥션 풀을 데운 뒤 N번 순서대로 호출한다
# - 출력: 응답 크기(바이트), 평균·p50·p95·최소·최대(ms). 토큰은 출력하지 않는다
set -euo pipefail

PATH_ARG="${1:?경로 필요 (예: /api/users/1/recipes)}"
BASE="${BASE:-http://localhost:18080}"
N="${N:-50}"
WARMUP="${WARMUP:-10}"
: "${TOKEN:?TOKEN 필요 (scripts/bench/dev-token.sh)}"

call() { curl -s -o /dev/null -w "$1" -H "Authorization: Bearer $TOKEN" "$BASE$PATH_ARG"; }

status=$(call '%{http_code}')
[ "$status" = 200 ] || { echo "HTTP $status: $PATH_ARG" >&2; exit 1; }
for _ in $(seq 1 "$WARMUP"); do call '' ; done

size=$(call '%{size_download}')
times=$(for _ in $(seq 1 "$N"); do call '%{time_total}\n'; done)

echo "$times" | sort -n | awk -v n="$N" -v size="$size" -v path="$PATH_ARG" '
  { t[NR] = $1 * 1000; sum += t[NR] }
  END {
    p50 = t[int((n + 1) * 0.50)]; p95 = t[int((n + 1) * 0.95)]
    printf "%s  size=%dB  n=%d  avg=%.1fms  p50=%.1fms  p95=%.1fms  min=%.1fms  max=%.1fms\n",
      path, size, n, sum / n, p50, p95, t[1], t[n]
  }'
