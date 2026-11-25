#!/usr/bin/env bash
# 在服务器执行：找出占磁盘的大头（只读诊断，不删除）
#   bash scripts/diagnose-disk-on-server.sh
set -euo pipefail

say() { printf '%s\n' "$*"; }
hr() { say "----------------------------------------"; }

hr
say "== 总览 =="
df -hT / /www 2>/dev/null || df -hT /

hr
say "== /www 下一级目录 =="
du -xh --max-depth=1 /www 2>/dev/null | sort -hr | head -20

hr
say "== /www/wwwroot 下一级 =="
du -xh --max-depth=1 /www/wwwroot 2>/dev/null | sort -hr | head -20

hr
say "== learn-space 下一级 =="
if [[ -d /www/wwwroot/learn-space ]]; then
  du -xh --max-depth=1 /www/wwwroot/learn-space 2>/dev/null | sort -hr | head -25
fi

hr
say "== Docker =="
if command -v docker >/dev/null 2>&1; then
  docker system df || true
  say ""
  say "-- 镜像列表 --"
  docker images --format 'table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.ID}}' 2>/dev/null | head -40 || true
  say ""
  say "-- 数据卷 --"
  docker volume ls 2>/dev/null || true
  say ""
  du -xh --max-depth=1 /var/lib/docker 2>/dev/null | sort -hr | head -15 || true
else
  say "未安装 docker 命令"
fi

hr
say "== 常见大目录 =="
for p in /www/backup /www/wwwlogs /www/server /var/log /var/lib/docker /tmp; do
  if [[ -e "$p" ]]; then
    du -sh "$p" 2>/dev/null || true
  fi
done

hr
say "== 系统里大于 300MB 的目录（抽样，可能稍慢）=="
du -xh --max-depth=2 /www /var/lib/docker 2>/dev/null | awk '$1 ~ /G$/ || ($1 ~ /M$/ && $1+0 >= 300)' | sort -hr | head -40

hr
say "诊断结束。把以上输出发给维护者即可。"
