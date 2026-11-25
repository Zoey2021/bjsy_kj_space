#!/usr/bin/env bash
# 紧急腾磁盘（安全版）：不动 MySQL/业务数据卷，不 docker compose down -v
# 在服务器执行：
#   cd /www/wwwroot/learn-space && YES=1 bash scripts/free-disk-now.sh
set -euo pipefail

ROOT="${1:-/www/wwwroot/learn-space}"
YES="${YES:-0}"
cd "$ROOT" 2>/dev/null || cd /www/wwwroot/learn-space

say() { printf '%s\n' "$*"; }
hr() { say "----------------------------------------"; }
confirm() {
  if [[ "$YES" == "1" ]]; then return 0; fi
  read -r -p "$1 (y/N) " ans || true
  [[ "${ans:-}" == "y" || "${ans:-}" == "Y" ]]
}

hr
say "==> 清盘前"
df -h / | tail -1
if command -v docker >/dev/null 2>&1; then
  docker system df || true
fi

hr
say "==> 1) Docker：构建缓存 + 悬空镜像/容器/网络（不删 volume）"
if confirm "清理 Docker 缓存与无用镜像？"; then
  # 构建层往往是大头
  docker builder prune -af || true
  # 悬空镜像
  docker image prune -af || true
  # 已停止容器、无用网络（不带 -v，不会删数据卷）
  docker container prune -f || true
  docker network prune -f || true
  # 不执行：docker volume prune / docker compose down -v
  say "Docker 缓存清理完成。"
else
  say "跳过 Docker。"
fi

hr
say "==> 2) 项目根目录旧部署 zip / 本地大包"
if confirm "删除 learn-space 根目录 *.zip 与常见大临时包？"; then
  find "$ROOT" -maxdepth 1 -type f \( -name '*.zip' -o -name '*.tar' -o -name '*.tar.gz' \) -print -delete 2>/dev/null || true
  # Maven 本地仓若误放在项目里
  if [[ -d "$ROOT/backend/.m2" ]]; then
    du -sh "$ROOT/backend/.m2" || true
    rm -rf "$ROOT/backend/.m2"
    say "已删 backend/.m2"
  fi
  if [[ -d "$ROOT/frontend/node_modules" ]]; then
    # 镜像构建用，运行时不需要宿主机 node_modules
    du -sh "$ROOT/frontend/node_modules" || true
    if confirm "删除宿主机 frontend/node_modules（容器内另有，一般可删）？"; then
      rm -rf "$ROOT/frontend/node_modules"
      say "已删 frontend/node_modules"
    fi
  fi
else
  say "跳过 zip。"
fi

hr
say "==> 3) 本机 school/textbooks 大文件（访问已走 OSS）"
OSS="${OSS_BASE:-https://learn-space-static.oss-cn-beijing.aliyuncs.com}"
code_pdf=$(curl -sS -o /dev/null -w '%{http_code}' -I -m 15 "$OSS/textbooks/grade6-up.pdf" || true)
if [[ "$code_pdf" == "200" ]]; then
  if [[ -d "$ROOT/frontend/public/school" ]]; then
    du -sh "$ROOT/frontend/public/school" || true
    if confirm "清空本机 school 副本？"; then
      rm -rf "$ROOT/frontend/public/school"
      mkdir -p "$ROOT/frontend/public/school"
      say "已清空 school"
    fi
  fi
  if [[ -d "$ROOT/frontend/public/textbooks" ]]; then
    du -sh "$ROOT/frontend/public/textbooks" || true
    if confirm "删除本机 textbooks PDF？"; then
      find "$ROOT/frontend/public/textbooks" -type f -name '*.pdf' -delete
      say "已删 textbooks PDF"
    fi
  fi
else
  say "OSS 校验失败(HTTP $code_pdf)，跳过删课件，避免打不开。"
fi

hr
say "==> 4) 系统日志（谨慎截断，不删业务）"
if confirm "真空 journal 日志到近 3 天？"; then
  journalctl --vacuum-time=3d 2>/dev/null || true
fi
# 宝塔站点日志过大时可截断（保留文件）
if [[ -d /www/wwwlogs ]]; then
  du -sh /www/wwwlogs || true
  if confirm "清空 /www/wwwlogs 下超过 50MB 的日志内容（保留文件名）？"; then
    find /www/wwwlogs -type f -size +50M -exec truncate -s 0 {} \; 2>/dev/null || true
    say "已截断大日志"
  fi
fi

hr
say "==> 5) /root 与 /tmp 常见垃圾"
if confirm "清理 /tmp 下超过 7 天的临时文件？"; then
  find /tmp -xdev -type f -mtime +7 -delete 2>/dev/null || true
fi
# Docker 构建偶发落到 /root
if [[ -d /root/.cache ]]; then
  du -sh /root/.cache 2>/dev/null || true
  if confirm "清理 /root/.cache？"; then
    rm -rf /root/.cache/*
    say "已清理 /root/.cache"
  fi
fi

hr
say "==> 清盘后"
df -h / | tail -1
if command -v docker >/dev/null 2>&1; then
  docker system df || true
  say ""
  say "当前业务容器："
  docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Image}}' || true
fi
say ""
say "未删除：MySQL 数据卷、学生成绩、正在用的课次页、Docker volume。"
say "若磁盘仍紧：再执行 bash scripts/diagnose-disk-on-server.sh 把输出发来。"
