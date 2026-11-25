#!/usr/bin/env bash
# 在服务器 /www/wwwroot/learn-space 执行：
# 1) 上传并解压 scratch-community-*.zip 覆盖本目录
# 2) bash apply-scratch-community.sh
set -euo pipefail
ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"

need=(
  frontend/src/constants/techSpace.js
  frontend/src/views/teacher/ScratchReview.vue
  frontend/src/views/student/ScratchCommunity.vue
  backend/src/main/java/com/itech/learnspace/controller/ScratchCommunityController.java
  frontend/public/scratch-player/index.html
)
for f in "${need[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "缺少 $f，请先解压部署包"
    exit 1
  fi
done
if ! grep -q '编程作品社区' frontend/src/constants/techSpace.js; then
  echo "techSpace.js 未更新到「编程作品社区」"
  exit 1
fi
if ! grep -q 'teacher/publish' backend/src/main/java/com/itech/learnspace/controller/ScratchCommunityController.java; then
  echo "后端缺少教师上传接口"
  exit 1
fi

echo "重建 frontend + backend…"
docker compose build --no-cache frontend backend
docker compose up -d frontend backend

# nginx.conf 已挂载，重启前端容器即可加载新反代规则
docker compose up -d --force-recreate frontend

echo "完成。"
echo "学生：科技空间 → 编程作品社区（浏览 / 试玩 / 点赞）"
echo "教师：科技世界 → 编程作品社区（上传发布 + 审核学生投稿）"
echo "若旧作品仍 Failed to fetch：请老师在「已发布」里撤回后重新上传，或确认文件在 volume 中。"
