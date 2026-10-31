#!/bin/bash

# 一键配置Dify公网访问
# 使用方法: ./一键配置Dify.sh

echo "=========================================="
echo "Dify公网访问配置向导"
echo "=========================================="
echo ""

# 检查是否提供了ngrok地址
if [ -z "$1" ]; then
    echo "请先在你的本地电脑上运行 ngrok http 80"
    echo "然后复制ngrok显示的地址（例如: https://abc123.ngrok-free.app）"
    echo ""
    read -p "请输入ngrok地址: " NGROK_URL
else
    NGROK_URL="$1"
fi

if [ -z "$NGROK_URL" ]; then
    echo "❌ 错误：未提供ngrok地址"
    exit 1
fi

# 移除末尾的斜杠
NGROK_URL=$(echo "$NGROK_URL" | sed 's|/$||')

echo ""
echo "请选择Dify的路径："
echo "1. /chat/DNGyJpSDOa9BsC0H (默认)"
echo "2. /chatbot/DNGyJpSDOa9BsC0H"
read -p "请选择 (1 或 2，默认1): " PATH_CHOICE

if [ "$PATH_CHOICE" = "2" ]; then
    DIFY_PATH="/chatbot/DNGyJpSDOa9BsC0H"
else
    DIFY_PATH="/chat/DNGyJpSDOa9BsC0H"
fi

FULL_URL="${NGROK_URL}${DIFY_PATH}"

echo ""
echo "=========================================="
echo "配置信息"
echo "=========================================="
echo "ngrok地址: $NGROK_URL"
echo "Dify路径: $DIFY_PATH"
echo "完整URL: $FULL_URL"
echo ""

read -p "确认配置？(y/n): " CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo "已取消"
    exit 0
fi

# 检查.env文件
if [ ! -f ".env" ]; then
    echo "⚠️  .env文件不存在，正在创建..."
    touch .env
fi

# 更新配置
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

# 询问是否重启服务
read -p "是否立即重启服务？(y/n): " RESTART
if [ "$RESTART" = "y" ] || [ "$RESTART" = "Y" ]; then
    echo "正在重启服务..."
    sudo systemctl restart game-app
    sleep 2
    echo ""
    echo "服务状态:"
    sudo systemctl status game-app --no-pager | head -10
fi

echo ""
echo "=========================================="
echo "下一步"
echo "=========================================="
echo "1. 确保本地电脑上的ngrok一直在运行"
echo "2. 在其他电脑上测试访问:"
echo "   http://123.57.59.156:3000/ai-study?team_id=你的团队ID"
echo ""

