#!/usr/bin/env bash
# Deploy / cập nhật CareerCompass trên VPS.
# Dùng cho các lần deploy LẶP LẠI (lần đầu xem DEPLOY.md).
#
#   cd ~/lap-trinh-java && ./deploy/deploy.sh
#
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "LỖI: thiếu file .env (copy từ .env.example rồi điền secret)." >&2
  exit 1
fi

echo "==> Kéo code mới nhất"
git pull --ff-only

echo "==> Build image + khởi động lại"
docker compose up -d --build

echo "==> Chờ app khởi động"
for i in $(seq 1 60); do
  if curl -fsS -o /dev/null http://127.0.0.1:"${APP_PORT:-8080}"/login; then
    echo "==> OK: app trả HTTP 200 ở /login"
    docker compose ps
    exit 0
  fi
  sleep 5
done

echo "LỖI: app không phản hồi sau 5 phút. Log 50 dòng cuối:" >&2
docker compose logs app --tail 50 >&2
exit 1
