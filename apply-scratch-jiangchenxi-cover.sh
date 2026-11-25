#!/usr/bin/env bash
# 江辰兮作品展示：标题「勇闯星际」、作者 2021级9班、黑色封面
# 在服务器 /www/wwwroot/learn-space：
#   unzip -o scratch-jiangchenxi-cover.zip && bash apply-scratch-jiangchenxi-cover.sh
set -euo pipefail
ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"

need=(
  frontend/public/scratch-samples/jiangchenxi-cover.png
  frontend/public/scratch-samples/jiangchenxi.sb3
  backend/src/main/java/com/itech/learnspace/service/ScratchCommunityService.java
)
for f in "${need[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "缺少 $f"
    exit 1
  fi
done

echo "重建 backend（启动时回写标题/作者/封面）…"
docker compose build backend
docker compose up -d --force-recreate backend

echo "等待 backend…"
sleep 35
docker compose logs --tail=30 backend | grep -E 'Scratch|jiangchenxi|Started LearnSpace|ERROR' || true

echo "完成。强制刷新编程作品社区，应看到："
echo "  标题：勇闯星际"
echo "  作者：江辰兮 · 2021级9班"
echo "  封面：黑色星际截图"
