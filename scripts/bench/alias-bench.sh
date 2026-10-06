#!/usr/bin/env bash
# 동의어 사전 저장 방식 비교 측정 (메모리 HashMap vs MySQL 인덱스, AliasLookupBenchmark)
# 사용 (backend/에서): bash scripts/bench/alias-bench.sh   (BENCH_N, BENCH_WARMUP으로 횟수 조정)
# - 앱과 같은 리소스 제한(D-009: cpus 2.0, memory 1g) 컨테이너에서 compose MySQL(같은 제한)에 docker 네트워크로 접속한다
# - 측정용 임시 테이블 bench_ingredient_alias를 만들고 끝나면 지운다. DB 접속 정보는 .env에서 읽고 출력하지 않는다
set -euo pipefail

[ -f .env ] || { echo ".env 없음 (backend/에서 실행)" >&2; exit 1; }
set -a; . ./.env; set +a

docker compose up -d mysql >/dev/null
for i in $(seq 1 30); do
  docker compose exec -T mysql sh -c 'mysqladmin ping -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent' >/dev/null 2>&1 && break
  sleep 2
done
NETWORK="$(docker compose ps -q mysql | xargs docker inspect -f '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}')"

rm -f build/reports/alias-bench/result.txt
MSYS_NO_PATHCONV=1 docker run --rm --cpus 2.0 --memory 1g --network "$NETWORK" \
  -e ALIAS_BENCH=1 -e BENCH_N="${BENCH_N:-2000}" -e BENCH_WARMUP="${BENCH_WARMUP:-300}" \
  -e BENCH_DB_URL="jdbc:mysql://mysql:3306/${DB_NAME}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8" \
  -e BENCH_DB_USER="$DB_USERNAME" -e BENCH_DB_PASSWORD="$DB_PASSWORD" \
  -v "$(pwd -W 2>/dev/null || pwd)":/w -v cookit-gradle-cache:/root/.gradle -w /w eclipse-temurin:21-jdk \
  sh -c './gradlew cleanTest test --tests "*AliasLookupBenchmark" --no-daemon -q' >/dev/null   # cleanTest: 입력이 같아도 매번 다시 잰다

cat build/reports/alias-bench/result.txt
