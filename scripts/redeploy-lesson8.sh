#!/bin/bash
# 在项目根目录执行：bash scripts/redeploy-lesson8.sh
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> 项目目录: $ROOT"

if ! docker info >/dev/null 2>&1; then
  echo "请先启动 Docker Desktop，再重新运行本脚本。"
  exit 1
fi

echo "==> 执行数据库迁移（五年级下册第8课探究单）"
docker exec -i learn-mysql mysql -ulearnuser -plearnpass123 learn_space \
  < database/migration_feixiang_g5_lesson8.sql

echo "==> 重新构建并启动前后端"
docker compose build backend frontend
docker compose up -d

echo "==> 完成。请刷新浏览器，进入「五年级下册 → 第8课 体验控制系统」"
