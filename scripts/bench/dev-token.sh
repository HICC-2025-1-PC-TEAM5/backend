#!/usr/bin/env bash
# 로컬 측정·확인용 access 토큰 발급 (D-012로 /api/users/**가 로그인 필수가 됨)
# 사용 (backend/에서): TOKEN=$(bash scripts/bench/dev-token.sh 1)
# - application.properties의 app.jwt.secret으로 HS256 서명. 토큰을 화면이나 파일에 남기지 않는다
# - tver는 member.token_version과 같아야 한다 (기본 0, 로그아웃하면 증가)
set -euo pipefail

MEMBER_ID="${1:?회원 id 필요}"
TVER="${TVER:-0}"
TTL="${TTL:-3600}"
PROPS="${PROPS:-src/main/resources/application.properties}"

SECRET=$(grep '^app.jwt.secret=' "$PROPS" | cut -d= -f2-)
[ -n "$SECRET" ] || { echo "app.jwt.secret 없음: $PROPS" >&2; exit 1; }

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }
NOW=$(date +%s)
HEADER=$(printf '{"alg":"HS256","typ":"JWT"}' | b64url)
PAYLOAD=$(printf '{"sub":"%s","tver":%s,"mid":%s,"iat":%s,"exp":%s}' \
  "$MEMBER_ID" "$TVER" "$MEMBER_ID" "$NOW" "$((NOW + TTL))" | b64url)
SIG=$(printf '%s.%s' "$HEADER" "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" -binary | b64url)
printf '%s.%s.%s' "$HEADER" "$PAYLOAD" "$SIG"
