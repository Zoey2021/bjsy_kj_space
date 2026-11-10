#!/usr/bin/env bash
# 在服务器项目根执行：bash fix-g6-lesson7-on-server.sh
set -euo pipefail
ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"
echo "项目目录: $(pwd)"

if [[ ! -f frontend/public/lessons/g6-up-lesson7/index.html ]]; then
  echo "缺少 frontend/public/lessons/g6-up-lesson7/index.html"
  echo "请先上传 g6-up-lesson7-*.zip 并解压覆盖到本目录"
  exit 1
fi
if ! grep -q '算法小助手' frontend/public/lessons/g6-up-lesson7/index.html; then
  echo "HTML 文件不像新版第7课，请重新解压部署包"
  exit 1
fi
echo "HTML 已就绪"

if [[ ! -f database/bind_g6_up_lesson7.sql ]]; then
  echo "缺少 database/bind_g6_up_lesson7.sql"
  exit 1
fi

echo "写入数据库绑定…"
docker exec -i learn-mysql mysql -ulearnuser -plearnpass123 learn_space < database/bind_g6_up_lesson7.sql

echo "重建 frontend + backend…"
docker compose build --no-cache frontend backend
docker compose up -d frontend backend

echo "完成。请用学生账号打开六年级上册第7课，或教师端预览 /teacher/lesson/<课时id>"
echo "若仍只有介绍文，强制刷新浏览器后再试。"
