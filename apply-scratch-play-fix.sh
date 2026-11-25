#!/usr/bin/env bash
# 修复：学生端可在线试玩他人作品；去掉下载按钮
# 在服务器 /www/wwwroot/learn-space 执行：
#   unzip -o scratch-play-fix.zip && bash apply-scratch-play-fix.sh
set -euo pipefail
ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"

need=(
  frontend/public/scratch-player/index.html
  frontend/public/scratch-samples/jiangchenxi.sb3
  frontend/src/views/student/ScratchProjectDetail.vue
  frontend/src/views/teacher/ScratchReview.vue
  frontend/nginx.conf
  backend/src/main/java/com/itech/learnspace/service/ScratchCommunityService.java
  backend/src/main/java/com/itech/learnspace/config/SecurityConfig.java
  docker-compose.yml
)
for f in "${need[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "缺少 $f，请先解压部署包"
    exit 1
  fi
done

# nginx 读静态样例需要其他人可读
chmod a+r frontend/public/scratch-samples/*.sb3 2>/dev/null || true

if ! grep -q 'scratch-samples:/app/scratch-samples' docker-compose.yml; then
  echo "docker-compose.yml 未挂载 /app/scratch-samples"
  exit 1
fi
if ! grep -q 'location ^~ /scratch-samples/' frontend/nginx.conf; then
  echo "nginx.conf 缺少 /scratch-samples/ 配置"
  exit 1
fi
if ! grep -q '/api/scratch/projects/\*/sb3' backend/src/main/java/com/itech/learnspace/config/SecurityConfig.java; then
  echo "SecurityConfig 未放行已发布作品 sb3"
  exit 1
fi

echo "重建 frontend + backend…"
docker compose build frontend backend
docker compose up -d --force-recreate backend frontend

echo "等待 backend 起来…"
sleep 28
docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -E 'learn-backend|learn-frontend|learn-mysql' || true
docker compose logs --tail=30 backend | grep -E 'Scratch|江辰兮|Started LearnSpace|ERROR' || true

echo "完成。请学生强制刷新后：科技空间 → 编程作品社区 → 打开作品试玩（无下载按钮）。"
