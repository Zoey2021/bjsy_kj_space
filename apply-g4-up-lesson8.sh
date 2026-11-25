#!/usr/bin/env bash
# 在服务器 /www/wwwroot/learn-space 执行：
# 1) 上传并解压 g4-up-lesson8-*.zip 覆盖本目录
# 2) bash apply-g4-up-lesson8.sh
set -euo pipefail
ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"

if [[ ! -f frontend/public/lessons/g4-up-lesson8/index.html ]]; then
  echo "缺少 frontend/public/lessons/g4-up-lesson8/index.html"
  exit 1
fi
if [[ ! -f backend/src/main/resources/seed/g4-up-lesson8-workspace.json ]]; then
  echo "缺少 g4-up-lesson8-workspace.json"
  exit 1
fi

if [[ -f database/bind_g4_up_lesson8.sql ]]; then
  echo "写入数据库绑定…"
  docker exec -i learn-mysql mysql -ulearnuser -plearnpass123 learn_space < database/bind_g4_up_lesson8.sql
fi

echo "重建 frontend + backend（Seeder 会写入完整工作台配置）…"
docker compose build --no-cache frontend backend
docker compose up -d frontend backend

echo "完成。学生打开四年级上册第8课：上交作品 → 课堂小测 → 学习评价。"
