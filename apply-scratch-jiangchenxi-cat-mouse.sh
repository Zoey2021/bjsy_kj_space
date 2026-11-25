#!/usr/bin/env bash
# 上架江辰兮第二作品「猫抓老鼠」
# 在服务器 /www/wwwroot/learn-space：
#   unzip -o scratch-jiangchenxi-cat-mouse.zip && bash apply-scratch-jiangchenxi-cat-mouse.sh
set -euo pipefail
ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"

need=(
  frontend/public/scratch-samples/jiangchenxi-cat-mouse.sb3
  frontend/public/scratch-samples/jiangchenxi-cat-mouse-cover.png
  backend/src/main/java/com/itech/learnspace/service/ScratchCommunityService.java
)
for f in "${need[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "缺少 $f"
    exit 1
  fi
done

echo "重建 backend（启动时幂等上架「猫抓老鼠」）…"
docker compose build backend
docker compose up -d --force-recreate backend

echo "等待 backend…"
sleep 35
docker compose logs --tail=40 backend | grep -E 'Scratch|Started LearnSpace|ERROR' || true

echo "完成。强制刷新编程作品社区，应看到江辰兮两件作品："
echo "  1) 勇闯星际"
echo "  2) 猫抓老鼠（作者 江辰兮 · 2021级9班，老师推荐）"
