#!/bin/bash
# 更新Dify地址 - 使用正确的ngrok地址

cd /www/wwwroot/game01

echo "=========================================="
echo "更新Dify Chatbot地址（正确地址）"
echo "=========================================="
echo ""

# 正确的ngrok地址（注意是.dev不是.de）
NGROK_URL="https://nonblamefully-unpawned-bobbi.ngrok-free.dev"
DIFY_URL="${NGROK_URL}/chat/DNGyJpSDOa9BsC0H"

echo "正确的Dify URL: $DIFY_URL"
echo ""

# 备份原配置
if [ -f ".env" ]; then
    cp .env .env.bak
    echo "✅ 已备份原配置到 .env.bak"
fi

# 更新配置
if grep -q "^DIFY_CHATBOT_URL" .env; then
    sed -i "s|^DIFY_CHATBOT_URL=.*|DIFY_CHATBOT_URL=${DIFY_URL}|" .env
    echo "✅ 已更新配置"
else
    echo "DIFY_CHATBOT_URL=${DIFY_URL}" >> .env
    echo "✅ 已添加配置"
fi

echo ""
echo "当前配置："
grep DIFY_CHATBOT_URL .env

echo ""
echo "=========================================="
echo "测试连接"
echo "=========================================="
echo ""

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 10 "$DIFY_URL" 2>/dev/null)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ] || [ "$HTTP_CODE" = "301" ]; then
    echo "✅ Dify服务可访问 (HTTP $HTTP_CODE)"
elif [ "$HTTP_CODE" = "403" ] || [ "$HTTP_CODE" = "404" ]; then
    echo "⚠️  返回HTTP $HTTP_CODE（可能需要浏览器验证）"
    echo "   请在浏览器中访问: $DIFY_URL"
    echo "   完成ngrok验证后，应该就能正常访问了"
else
    echo "⚠️  Dify服务无法访问 (HTTP $HTTP_CODE)"
    echo "   请确认："
    echo "   1. ngrok是否在本地Mac上运行"
    echo "   2. Dify服务是否在本地正常运行"
    echo "   3. 在浏览器中访问: $DIFY_URL 完成验证"
fi

echo ""
echo "=========================================="
echo "重启服务"
echo "=========================================="
echo ""

sudo systemctl restart game-app
sleep 2

if sudo systemctl is-active --quiet game-app; then
    echo "✅ 服务已重启"
    sudo systemctl status game-app --no-pager | head -10
else
    echo "❌ 服务启动失败"
    sudo systemctl status game-app --no-pager | head -20
fi

echo ""
echo "=========================================="
echo "配置完成"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 在浏览器中访问: $DIFY_URL"
echo "   完成ngrok验证（点击'Visit Site'按钮）"
echo ""
echo "2. 然后访问应用:"
echo "   http://123.57.59.156:3000/ai-study?team_id=你的团队ID"
echo ""
echo "3. 检查左侧AI学习助手是否正常加载"
echo ""
echo "⚠️  重要：保持ngrok在本地Mac上运行！"
echo ""

