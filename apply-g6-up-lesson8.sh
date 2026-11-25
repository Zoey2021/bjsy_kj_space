#!/usr/bin/env bash
# 部署六年级上第8课「算法的多样性」三探究 + ABC 支架
# 在服务器 /www/wwwroot/learn-space：
#   unzip -o g6-up-lesson8.zip && bash apply-g6-up-lesson8.sh
set -euo pipefail
ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"

need=(
  frontend/public/lessons/g6-up-lesson8/index.html
  backend/src/main/resources/seed/g6-up-lesson8-workspace.json
  backend/src/main/resources/seed/g6-up-lesson8-scaffold.json
  backend/src/main/java/com/itech/learnspace/config/G6UpLesson8Seeder.java
  backend/src/main/java/com/itech/learnspace/config/G6UpLesson567ScaffoldSeeder.java
  docker-compose.yml
)
for f in "${need[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "缺少 $f"
    exit 1
  fi
done

if ! grep -q 'g6-up-lesson8' docker-compose.yml; then
  echo "docker-compose.yml 未挂载 g6-up-lesson8"
  exit 1
fi

echo "重建 frontend + backend（工作台/支架 + 课件挂载）…"
docker compose build frontend backend
docker compose up -d --force-recreate backend frontend

echo "等待 backend…"
sleep 35
docker compose logs --tail=40 backend | grep -E '第8课|lesson8|ABC|Started LearnSpace|ERROR' || true

echo "完成。学生打开：六年级上册 → 第二单元 → 第8课 算法的多样性"
echo "应看到三个探究：折半涂色卡 / 流程图补全 / 两种算法跑一跑，并有 ABC 支架。"
