#!/usr/bin/env bash
# 在服务器 /www/wwwroot/learn-space 执行：
# 1) 六年级上第1–4课互动资源换成轻量「已过期」页
# 2) 删除已走 OSS 的本机 school 课件副本（通常可腾出数 GB）
# 3) 可选清理 Docker 无置镜像/构建缓存
set -euo pipefail

ROOT="${1:-/www/wwwroot/learn-space}"
cd "$ROOT"

echo "==> 当前磁盘"
df -h / | tail -1

echo "==> 归档并替换六年级上第1–4课静态资源"
ARCH="_archive/g6-up-lessons-1-4-$(date +%Y%m%d)"
mkdir -p "$ARCH"
for d in g6-up-lesson1 g6-up-lesson2 g6-up-lesson3 g6-up-lesson4; do
  SRC="frontend/public/lessons/$d"
  if [[ -d "$SRC" ]]; then
    # 若还不是占位页，先备份
    if [[ -f "$SRC/index.html" ]] && ! grep -q '已过期' "$SRC/index.html" 2>/dev/null; then
      rm -rf "$ARCH/$d"
      cp -a "$SRC" "$ARCH/$d"
      echo "  已备份 $d -> $ARCH/$d"
    fi
  fi
done

# 写入轻量占位（与仓库 stubs 一致；若仓库已带 stubs，解压覆盖即可）
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
  # 删掉占位以外的大文件（如 skulpt、图片）
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
  echo "  已写入占位 $d"
done

echo "==> 检查本机 school 课件（nginx 已 301 到 OSS，可安全删除本机副本）"
if [[ -d frontend/public/school ]]; then
  du -sh frontend/public/school || true
  read -r -p "删除 frontend/public/school 本机副本？(y/N) " ans || true
  if [[ "${ans:-}" == "y" || "${ans:-}" == "Y" ]]; then
    mkdir -p "_archive"
    # 不打包 pptx（太大）；直接删本机副本
    rm -rf frontend/public/school
    mkdir -p frontend/public/school
    echo "已删除 school 本机课件。访问仍走 OSS。"
  else
    echo "跳过删除 school。"
  fi
fi

echo "==> 可选：清理 Docker 构建缓存（不会删业务数据卷）"
read -r -p "执行 docker builder prune -f ？(y/N) " ans2 || true
if [[ "${ans2:-}" == "y" || "${ans2:-}" == "Y" ]]; then
  docker builder prune -f || true
fi

echo "==> 重建前端使「已过期」界面生效"
docker compose up -d --build frontend

echo "==> 完成后磁盘"
df -h / | tail -1
echo "说明：六年级前4课互动页本身很小；真正占空间的往往是 school 课件与 Docker 缓存。"
echo "学生端目录会显示「已过期」，无法再进入学习；教师预览不受限。"
