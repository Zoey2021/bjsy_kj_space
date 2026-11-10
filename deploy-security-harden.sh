#!/usr/bin/env bash
# 在项目根目录执行：生成安全加固部署包（不含密钥）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/security-harden-$(date +%Y%m%d-%H%M).zip"
cd "$ROOT"
zip -r "$OUT" \
  database/harden_passwords.sql \
  database/HARDEN_README.md \
  database/init.sql \
  database/restore_teacher_admin.sql \
  docker-compose.yml \
  backend/src/main/java/com/itech/learnspace/security/PasswordPolicy.java \
  backend/src/main/java/com/itech/learnspace/security/LoginThrottleService.java \
  backend/src/main/java/com/itech/learnspace/security/JwtAuthFilter.java \
  backend/src/main/java/com/itech/learnspace/service/AuthService.java \
  backend/src/main/java/com/itech/learnspace/service/AdminService.java \
  backend/src/main/java/com/itech/learnspace/service/SchoolManageService.java \
  backend/src/main/java/com/itech/learnspace/config/KechuangTeacherSeeder.java \
  frontend/src/views/Login.vue \
  frontend/src/api/request.js \
  -x '*.DS_Store'
echo "已生成: $OUT"
echo "上线顺序见 database/HARDEN_README.md"
