#!/usr/bin/env bash
# 한 API 요청이 실행하는 SQL 수 측정 (SQL 로그를 켠 앱을 따로 띄워 센다, 데이터를 바꾸지 않는다)
# 사용 (backend/에서): TOKEN=$(bash scripts/bench/dev-token.sh 1) bash scripts/bench/sql-count.sh /api/users/1/recipes [경로 ...]
# - compose의 app 이미지를 PROBE_PORT(기본 18081)로 띄우고 org.hibernate.SQL 로그 줄 수를 요청 전후로 비교한다. 끝나면 내린다
# - 앱 이미지를 바꿨다면 먼저 docker compose build app
set -euo pipefail

: "${TOKEN:?TOKEN 필요 (scripts/bench/dev-token.sh)}"
PORT="${PROBE_PORT:-18081}"
NAME=cookit-sqlprobe

docker rm -f "$NAME" >/dev/null 2>&1 || true
docker compose run -d --rm --name "$NAME" -p "$PORT:8080" \
  -e 'SPRING_APPLICATION_JSON={"logging":{"level":{"org.hibernate.SQL":"DEBUG"}}}' app >/dev/null
trap 'docker stop "$NAME" >/dev/null 2>&1 || true' EXIT
for _ in $(seq 1 60); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' -X POST "http://localhost:$PORT/api/auth/refresh")" = 401 ] && break
  sleep 2
done

count() { docker logs "$NAME" 2>&1 | grep -c "org.hibernate.SQL" || true; }
for p in "$@"; do
  curl -s -o /dev/null -H "Authorization: Bearer $TOKEN" "http://localhost:$PORT$p"   # 첫 호출(캐시 준비)은 세지 않는다
  sleep 1
  before=$(count)
  curl -s -o /dev/null -H "Authorization: Bearer $TOKEN" "http://localhost:$PORT$p"
  sleep 1
  echo "$p  sql=$(( $(count) - before ))"
done
