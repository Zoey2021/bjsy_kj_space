#!/usr/bin/env bash
# 在服务器 /www/wwwroot/learn-space 执行：
# 确认 OSS 可访问后，删除本机校本/教材大文件，释放磁盘（约 4～5GB+）
set -euo pipefail
cd "$(dirname "$0")/.."

OSS='https://learn-space-static.oss-cn-beijing.aliyuncs.com'

echo "== 检查 OSS =="
code_pdf=$(curl -sS -o /dev/null -w '%{http_code}' -I -m 20 "$OSS/textbooks/grade6-up.pdf" || true)
code_ppt=$(curl -sS -o /dev/null -w '%{http_code}' -I -m 20 "$OSS/school/kechuang-up/lesson-01.pptx" || true)
echo "OSS textbooks/grade6-up.pdf -> HTTP $code_pdf"
echo "OSS school/.../lesson-01.pptx -> HTTP $code_ppt"

if [[ "$code_pdf" != "200" || "$code_ppt" != "200" ]]; then
  echo "OSS 校验失败，中止删除，以免学生打不开课件。"
  exit 1
fi

echo "== 删除本机大目录（OSS 已有副本）=="
if [[ -d frontend/public/school/kechuang-up ]]; then
  du -sh frontend/public/school/kechuang-up || true
  rm -rf frontend/public/school/kechuang-up
  mkdir -p frontend/public/school
  echo "已删除 frontend/public/school/kechuang-up"
else
  echo "本机已无 school/kechuang-up"
fi

if [[ -d frontend/public/textbooks ]]; then
  du -sh frontend/public/textbooks || true
  # 只删 PDF，保留目录（避免以后误用挂载时报错）
  find frontend/public/textbooks -type f -name '*.pdf' -delete
  echo "已删除 frontend/public/textbooks/*.pdf"
else
  echo "本机已无 textbooks 目录"
fi

echo "== 磁盘 =="
df -h /
du -sh frontend/public/school frontend/public/textbooks 2>/dev/null || true
echo "完成。请确认：curl -I https://你的域名/textbooks/grade6-up.pdf 应返回 301 到 OSS。"
