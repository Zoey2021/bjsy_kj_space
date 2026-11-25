#!/usr/bin/env bash
# 学生端 Scratch 社区点赞：列表可点赞 + 详情按钮更醒目
# 在服务器 /www/wwwroot/learn-space：
#   unzip -o scratch-like.zip && bash apply-scratch-like.sh
set -euo pipefail
ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"

need=(
  frontend/src/views/student/ScratchCommunity.vue
  frontend/src/views/student/ScratchProjectDetail.vue
)
for f in "${need[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "缺少 $f，请先解压部署包"
    exit 1
  fi
done

echo "重建 frontend（Vue 页面打进镜像）…"
docker compose build frontend
docker compose up -d --force-recreate frontend

echo "完成。学生强制刷新后：社区卡片可直接点赞，详情页底部「给作品点赞」也可点。"
