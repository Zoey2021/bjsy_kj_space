#!/usr/bin/env bash
# 部署六年级上「第一单元小测」（学生提交 + 教师批改打分，学生仅见等级）
# 在服务器 /www/wwwroot/learn-space：
#   unzip -o g6-up-unit1-quiz.zip && bash apply-g6-up-unit1-quiz.sh
set -euo pipefail
ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"

need=(
  backend/src/main/resources/seed/g6-up-unit1-quiz.json
  backend/src/main/java/com/itech/learnspace/config/G6UpUnit1QuizSeeder.java
  backend/src/main/java/com/itech/learnspace/entity/UnitQuizSubmission.java
  backend/src/main/java/com/itech/learnspace/repository/UnitQuizSubmissionRepository.java
  backend/src/main/java/com/itech/learnspace/service/UnitQuizService.java
  backend/src/main/java/com/itech/learnspace/controller/LearnController.java
  backend/src/main/java/com/itech/learnspace/controller/TeacherController.java
  frontend/src/views/student/UnitQuizStudent.vue
  frontend/src/views/teacher/UnitQuizTeacher.vue
  frontend/src/utils/unitQuiz.js
  frontend/src/router/index.js
  frontend/src/api/index.js
  frontend/public/lessons/g6-up-unit1-quiz/student.html
  frontend/public/lessons/g6-up-unit1-quiz/teacher.html
)
for f in "${need[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "缺少 $f"
    exit 1
  fi
done

echo "重建 frontend + backend（第一单元小测）…"
docker compose build frontend backend
docker compose up -d --force-recreate backend frontend

echo "等待 backend…"
sleep 40
docker compose logs --tail=50 backend | grep -E '单元小测|Unit1Quiz|unit_quiz|Started LearnSpace|ERROR' || true

echo "完成。入口：六年级上册 → 第一单元 →「第一单元小测」"
echo "教师：查看提交并打分；学生：提交后仅见 A/B/C/D 等级与评语。"
echo "正式试题稍后替换 seed/g6-up-unit1-quiz.json 后重建 backend 即可。"
