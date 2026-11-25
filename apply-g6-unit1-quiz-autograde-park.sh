#!/usr/bin/env bash
# 单元小测：提交即自动批改 + 完成后可申请游学乐园
# 在服务器 /www/wwwroot/learn-space：
#   unzip -o g6-up-unit1-quiz-autograde-park.zip && bash apply-g6-unit1-quiz-autograde-park.sh
set -euo pipefail
ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"

need=(
  backend/src/main/java/com/itech/learnspace/service/UnitQuizService.java
  backend/src/main/java/com/itech/learnspace/service/ParkService.java
  backend/src/main/resources/seed/g6-up-unit1-quiz.json
  frontend/src/views/student/UnitQuizStudent.vue
  frontend/src/views/teacher/UnitQuizTeacher.vue
)
for f in "${need[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "缺少 $f"
    exit 1
  fi
done

echo "重建 frontend + backend…"
docker compose build frontend backend
docker compose up -d --force-recreate backend frontend

echo "等待 backend…"
sleep 40
docker compose logs --tail=40 backend | grep -E '单元小测|Unit1Quiz|Started LearnSpace|ERROR' || true

echo "完成。"
echo "1) 学生提交单元小测后立即看到等级（无需老师点批改）"
echo "2) 教师请把班级「当前课时」设为「第一单元小测」，已交学生即可申请游学乐园"
echo "3) 老师仍可在批改页微调分数"
