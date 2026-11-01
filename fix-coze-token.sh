#!/usr/bin/env bash
# 在服务器 /www/wwwroot/learn-space 执行：写入/校正小派老师扣子环境变量并重建 backend
# Token 请放在同目录 .env 的 COZE_CHAT_TOKEN，勿写进本脚本。
set -euo pipefail
cd "$(dirname "$0")"

URL='https://y67mq6w3zb.coze.site/stream_run'

if [[ ! -f .env ]]; then
  echo "缺少 .env，请先创建并填写 COZE_CHAT_TOKEN"
  exit 1
fi

# shellcheck disable=SC1091
set -a
# 只导出 COZE_ 相关，避免 .env 其它行语法问题
eval "$(grep -E '^COZE_' .env | sed 's/\r$//')"
set +a

TOKEN="${COZE_CHAT_TOKEN:-}"
if [[ -z "$TOKEN" ]]; then
  echo "请在 .env 中设置 COZE_CHAT_TOKEN 后再运行"
  exit 1
fi

PUBLIC_ORIGIN="${COZE_PUBLIC_ORIGIN:-https://bjsysmartedu.cn}"

if grep -q '^COZE_CHAT_API_URL=' .env; then
  sed -i.bak "s|^COZE_CHAT_API_URL=.*|COZE_CHAT_API_URL=${URL}|" .env
else
  echo "COZE_CHAT_API_URL=${URL}" >> .env
fi
if grep -q '^COZE_PUBLIC_ORIGIN=' .env; then
  sed -i.bak "s|^COZE_PUBLIC_ORIGIN=.*|COZE_PUBLIC_ORIGIN=${PUBLIC_ORIGIN}|" .env
else
  echo "COZE_PUBLIC_ORIGIN=${PUBLIC_ORIGIN}" >> .env
fi
rm -f .env.bak

echo "已校正 .env 中的 COZE_CHAT_API_URL / COZE_PUBLIC_ORIGIN（Token 保持原值）"
grep '^COZE_' .env | sed 's/TOKEN=.*/TOKEN=***/'

docker compose up -d --build --force-recreate backend
docker compose exec -T backend sh -c 'echo URL=$COZE_CHAT_API_URL; echo TOKEN_LEN=${#COZE_CHAT_TOKEN}; echo TOKEN_PREFIX=$(printf %s "$COZE_CHAT_TOKEN" | cut -c1-8)'
echo "完成。请强制刷新算法小助手页面再试。"
