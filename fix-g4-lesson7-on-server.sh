#!/usr/bin/env bash
# 在服务器 /www/wwwroot/learn-space 执行：绑定四年级上第7课并刷新前端
set -euo pipefail
ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"

if [[ -f database/migration_g4_up_lesson7.sql ]]; then
  echo "==> 执行 SQL 绑定（需本机 mysql 客户端且能连库）"
  # 按实际账号改：默认与 docker-compose 一致时可改用 docker exec
  if docker compose ps mysql 2>/dev/null | grep -q Up; then
    docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" learn_space < database/migration_g4_up_lesson7.sql
  else
    echo "跳过自动 SQL：请在宝塔手动导入 database/migration_g4_up_lesson7.sql"
  fi
fi

echo "==> 重建 backend（加载 Seeder）+ frontend（挂载课页）"
docker compose up -d --build backend frontend

echo "完成。请打开「四年级上册 · 第7课 数据计算」强刷验证探究一～三。"
