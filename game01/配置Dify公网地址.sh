#!/bin/bash

# 配置Dify公网访问地址脚本
# 使用方法: ./配置Dify公网地址.sh https://你的ngrok地址.ngrok-free.app

if [ -z "$1" ]; then
    echo "=========================================="
    echo "配置Dify公网访问地址"
    echo "=========================================="
    echo ""
    echo "使用方法:"
    echo "  ./配置Dify公网地址.sh https://你的ngrok地址.ngrok-free.app"
    echo ""
    echo "示例:"
    echo "  ./配置Dify公网地址.sh https://abc123.ngrok-free.app"
    echo ""
    echo "请先在你的本地电脑上运行 ngrok http 80"
    echo "然后复制ngrok显示的地址（例如: https://abc123.ngrok-free.app）"
    echo ""
    exit 1
fi

NGROK_URL="$1"
# 注意：根据你的Dify实际路径选择，可能是 /chat/ 或 /chatbot/
# 如果路径不对，可以手动修改.env文件
DIFY_PATH="/chat/DNGyJpSDOa9BsC0H"
FULL_URL="${NGROK_URL}${DIFY_PATH}"

# 提示用户确认路径
echo "⚠️  请确认Dify路径是否正确："
echo "   当前使用: $DIFY_PATH"
echo "   如果路径不对，请手动编辑 .env 文件修改 DIFY_CHATBOT_URL"
echo ""

echo "=========================================="
echo "配置Dify公网访问地址"
echo "=========================================="
echo ""
echo "ngrok地址: $NGROK_URL"
echo "Dify路径: $DIFY_PATH"
echo "完整URL: $FULL_URL"
echo ""

# 检查.env文件是否存在
if [ ! -f ".env" ]; then
    echo "⚠️  .env文件不存在，正在创建..."
    touch .env
fi

# 更新或添加DIFY_CHATBOT_URL
if grep -q "^DIFY_CHATBOT_URL" .env; then
    echo "✅ 找到现有配置，正在更新..."
    sed -i "s|^DIFY_CHATBOT_URL=.*|DIFY_CHATBOT_URL=${FULL_URL}|" .env
else
    echo "✅ 添加新配置..."
    echo "DIFY_CHATBOT_URL=${FULL_URL}" >> .env
fi

echo ""
echo "=========================================="
echo "配置完成"
echo "=========================================="
echo ""
echo "当前配置:"
grep DIFY_CHATBOT_URL .env
echo ""
echo "下一步："
echo "1. 重启服务: sudo systemctl restart game-app"
echo "2. 检查状态: sudo systemctl status game-app"
echo "3. 测试访问: http://123.57.59.156:3000/ai-study?team_id=你的团队ID"
echo ""

