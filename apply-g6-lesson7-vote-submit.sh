#!/usr/bin/env bash
# 在服务器 /www/wwwroot/learn-space 执行：
# 1) 先上传并解压 g6-lesson7-vote-submit-20261116.zip 覆盖本目录
# 2) bash apply-g6-lesson7-vote-submit.sh
set -euo pipefail
ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"

if ! grep -q '请到课程总结下方提交' frontend/public/lessons/g6-up-lesson7/index.html 2>/dev/null; then
  echo "未检测到新版探究四页面，请先解压 g6-lesson7-vote-submit-*.zip 到本目录"
  exit 1
fi
if grep -q '重新开始探究' frontend/public/lessons/g6-up-lesson7/index.html; then
  echo "页面仍含「重新开始探究」，解压可能未覆盖成功"
  exit 1
fi

echo "重建 frontend + backend…"
docker compose build --no-cache frontend backend
docker compose up -d frontend backend
echo "完成。学生端：探究四选观点 → 课程总结下「提交观点」。"
echo "教师端：班级学情 → 本课探究四条形图与名单。"
