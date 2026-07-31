#!/usr/bin/env bash
# Deploy CareerCompass trên VPS THỦ CÔNG.
#
# Bình thường GitHub Actions đã tự deploy khi push vào main (xem .github/workflows/ci.yml).
# Script này để dùng khi CI hỏng, hoặc muốn deploy tay:
#
#   ./deploy/deploy.sh              # pull image mới nhất từ GHCR (giống CI)
#   ./deploy/deploy.sh --build      # build tại chỗ trên VPS (chậm, tốn RAM)
#
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "LỖI: thiếu file .env (copy từ .env.example rồi điền secret)." >&2
  exit 1
fi

MODE="${1:-pull}"

echo "==> Kéo code mới nhất"
git pull --ff-only

if [ "$MODE" = "--build" ]; then
  echo "==> Build image tại chỗ"
  # Bỏ APP_IMAGE để compose dùng tag local thay vì cố pull từ GHCR.
  sed -i '/^APP_IMAGE=/d' .env
  docker compose up -d --build
else
  echo "==> Pull image từ registry"
  # Cần đăng nhập trước nếu package để private:
  #   echo <GITHUB_PAT> | docker login ghcr.io -u <username> --password-stdin
  docker compose pull app
  docker compose up -d --no-build
fi

echo "==> Chờ app khởi động"
# tr -cd '0-9' bỏ mọi ký tự không phải chữ số: chặn CR (.env kiểu CRLF từ Windows),
# dấu nháy, khoảng trắng - những thứ khiến curl báo "(3) URL bad/illegal format".
PORT=$(sed -n 's/^APP_PORT=//p' .env | tail -n1 | tr -cd '0-9')
[ -n "$PORT" ] || PORT=8080

for i in $(seq 1 60); do
  if curl -fsS -o /dev/null "http://127.0.0.1:$PORT/login"; then
    echo "==> OK: app trả HTTP 200 ở /login"
    docker compose ps
    docker image prune -f
    exit 0
  fi
  sleep 5
done

echo "LỖI: app không phản hồi sau 5 phút. Log 50 dòng cuối:" >&2
docker compose logs app --tail 50 >&2
exit 1
