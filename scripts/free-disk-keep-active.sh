#!/usr/bin/env bash
# 在服务器 /www/wwwroot/learn-space 执行：
# 在不影响常用资源（数据库、在用课次、OSS 跳转）的前提下腾磁盘。
#
# 默认会做：
#   1) 先校验 OSS，再删本机 school / textbooks 大文件副本
#   2) 六年级上第1–4课仅保留轻量「已过期」页（删 skulpt/图片等附属文件）
#   3) 清理 Docker 构建缓存与悬空镜像（不删业务数据卷）
#   4) 清理项目根目录旧部署 zip（可关）
#
# 不会动：
#   - MySQL 数据卷 / 学生成绩与账号
#   - 四年级课、六年级第5～7课互动页
#   - .env、nginx 配置、backend 源码
set -euo pipefail

ROOT="${1:-/www/wwwroot/learn-space}"
OSS="${OSS_BASE:-https://learn-space-static.oss-cn-beijing.aliyuncs.com}"
CLEAN_ZIPS="${CLEAN_ZIPS:-1}"
YES="${YES:-0}"

cd "$ROOT"

say() { printf '%s\n' "$*"; }
hr() { say "----------------------------------------"; }

confirm() {
  local tip="$1"
  if [[ "$YES" == "1" ]]; then
    return 0
  fi
  read -r -p "$tip (y/N) " ans || true
  [[ "${ans:-}" == "y" || "${ans:-}" == "Y" ]]
}

hr
say "==> 清盘前磁盘"
df -h / | tail -1
say ""
say "==> 项目内大目录概览（便于对照）"
du -sh \
  frontend/public/school \
  frontend/public/textbooks \
  frontend/public/lessons \
  _archive \
  2>/dev/null || true
docker system df 2>/dev/null || true

hr
say "==> 1/4 校验 OSS（通过后才删本机课件副本）"
code_pdf=$(curl -sS -o /dev/null -w '%{http_code}' -I -m 20 "$OSS/textbooks/grade6-up.pdf" || true)
code_ppt=$(curl -sS -o /dev/null -w '%{http_code}' -I -m 20 "$OSS/school/kechuang-up/lesson-01.pptx" || true)
say "OSS textbooks/grade6-up.pdf -> HTTP $code_pdf"
say "OSS school/.../lesson-01.pptx -> HTTP $code_ppt"

if [[ "$code_pdf" == "200" && "$code_ppt" == "200" ]]; then
  if [[ -d frontend/public/school ]] && [[ -n "$(find frontend/public/school -type f 2>/dev/null | head -1)" ]]; then
    du -sh frontend/public/school || true
    if confirm "删除本机 frontend/public/school（访问仍走 OSS）？"; then
      rm -rf frontend/public/school
      mkdir -p frontend/public/school
      say "已删除 school 本机副本。"
    else
      say "跳过 school。"
    fi
  else
    say "本机 school 已空或不存在，跳过。"
  fi

  if [[ -d frontend/public/textbooks ]]; then
    du -sh frontend/public/textbooks || true
    if confirm "删除本机 textbooks 下 PDF（访问仍走 OSS）？"; then
      find frontend/public/textbooks -type f -name '*.pdf' -delete
      say "已删除 textbooks PDF。"
    else
      say "跳过 textbooks。"
    fi
  else
    say "本机无 textbooks 目录，跳过。"
  fi
else
  say "OSS 校验失败，跳过删除 school/textbooks，以免学生打不开课件。"
fi

hr
say "==> 2/4 六年级上第1–4课：只留「已过期」占位页"
ARCH="_archive/g6-up-lessons-1-4-$(date +%Y%m%d)"
mkdir -p "$ARCH"
for pair in \
  "g6-up-lesson1|第1课 算法与问题解决" \
  "g6-up-lesson2|第2课 抽象与建模" \
  "g6-up-lesson3|第3课 算法设计" \
  "g6-up-lesson4|第4课 用程序设计语言描述算法"
do
  d="${pair%%|*}"
  title="${pair#*|}"
  dir="frontend/public/lessons/$d"
  mkdir -p "$dir"
  if [[ -f "$dir/index.html" ]] && ! grep -q '已过期' "$dir/index.html" 2>/dev/null; then
    rm -rf "$ARCH/$d"
    cp -a "$dir" "$ARCH/$d"
    say "  已备份 $d -> $ARCH/$d"
  fi
  find "$dir" -mindepth 1 ! -name 'index.html' -exec rm -rf {} + 2>/dev/null || true
  cat > "$dir/index.html" <<EOF
<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${title}已过期</title>
<style>
body{margin:0;font-family:"PingFang SC","Microsoft YaHei",sans-serif;background:#f8fafc;color:#334155;display:grid;place-items:center;min-height:100vh}
.box{max-width:420px;padding:28px 24px;border:1px solid #e2e8f0;border-radius:16px;background:#fff;text-align:center}
.badge{display:inline-block;padding:4px 12px;border-radius:999px;background:#e2e8f0;color:#475569;font-size:13px;font-weight:700}
h1{font-size:18px;margin:14px 0 8px}p{margin:0;line-height:1.7;color:#64748b;font-size:14px}
</style></head><body><div class="box"><span class="badge">已过期</span>
<h1>${title}</h1>
<p>本课互动资源已收回，用于腾出服务器空间给后续新课。完成记录仍保留。</p>
</div></body></html>
EOF
  say "  已精简 $d"
done
say "未改动：g4-up-lesson*、g6-up-lesson5/6/7。"

hr
say "==> 3/4 Docker 缓存（不删 mysql_data 等业务卷）"
if confirm "执行 docker builder prune -f 与 docker image prune -f？"; then
  docker builder prune -f || true
  docker image prune -f || true
  say "Docker 缓存已清理。"
else
  say "跳过 Docker 清理。"
fi

hr
say "==> 4/4 项目根目录旧 zip（不影响运行）"
if [[ "$CLEAN_ZIPS" == "1" ]]; then
  zip_count=$(find . -maxdepth 1 -type f -name '*.zip' 2>/dev/null | wc -l | tr -d ' ')
  if [[ "${zip_count:-0}" -gt 0 ]]; then
    find . -maxdepth 1 -type f -name '*.zip' -print0 2>/dev/null | xargs -0 du -ch 2>/dev/null | tail -1 || true
    if confirm "删除项目根目录下 ${zip_count} 个 zip？"; then
      find . -maxdepth 1 -type f -name '*.zip' -delete
      say "已删除旧 zip。"
    else
      say "跳过 zip。"
    fi
  else
    say "根目录无 zip。"
  fi
else
  say "CLEAN_ZIPS=0，跳过。"
fi

hr
say "==> 清盘后磁盘"
df -h / | tail -1
docker system df 2>/dev/null || true
say ""
say "常用资源应仍正常："
say "  - 学生姓名/成绩：在 MySQL 数据卷，未动"
say "  - 四年级课、六年级第5～7课：未删"
say "  - 校本课件/教材 PDF：走 OSS 301"
say "建议抽查："
say "  curl -I https://你的域名/textbooks/grade6-up.pdf"
say "  curl -I https://你的域名/school/kechuang-up/lesson-01.pptx"
say "一键非交互示例：YES=1 bash scripts/free-disk-keep-active.sh"
