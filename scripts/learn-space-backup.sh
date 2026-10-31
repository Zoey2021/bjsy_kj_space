#!/bin/bash
# 在云服务器上导出数据库。网站文件由 iMac 用 rsync 增量同步，不必每次再打包。
set -euo pipefail
STAMP="$(date +%Y%m%d-%H%M)"
DEST="/www/backup/learn-space/${STAMP}"
mkdir -p "$DEST"
docker exec learn-mysql mysqldump -ulearnuser -plearnpass123 \
  --single-transaction --routines --databases learn_space \
  > "$DEST/learn_space.sql"
echo "数据库备份完成：$DEST/learn_space.sql"
ls -lh "$DEST/learn_space.sql"
ls -1dt /www/backup/learn-space/*/ 2>/dev/null | tail -n +9 | xargs -r rm -rf
