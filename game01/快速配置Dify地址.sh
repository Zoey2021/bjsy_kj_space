#!/bin/bash
# 快速配置Dify地址脚本

cd /www/wwwroot/game01

echo "=========================================="
echo "配置Dify服务地址"
echo "=========================================="
echo ""

# 检查当前配置
echo "当前配置："
grep DIFY_CHATBOT_URL .env || echo "未找到配置"
echo ""

# 提示用户输入
echo "请选择Dify服务地址："
echo ""
echo "1. Dify在同一服务器，使用localhost（需要先部署Dify）"
echo "2. Dify在其他服务器，使用IP地址"
echo "3. Dify使用域名/云服务"
echo "4. 手动输入地址"
echo ""
read -p "请选择 (1-4): " choice

case $choice in
    1)
        echo ""
        read -p "Dify服务端口 (默认5000): " port
        port=${port:-5000}
        DIFY_URL="http://localhost:${port}/chatbot/DNGyJpSDOa9BsC0H"
        ;;
    2)
        echo ""
        read -p "请输入Dify服务器IP: " ip
        read -p "请输入Dify服务端口 (默认5000): " port
        port=${port:-5000}
        DIFY_URL="http://${ip}:${port}/chatbot/DNGyJpSDOa9BsC0H"
        ;;
    3)
        echo ""
        read -p "请输入Dify域名或云服务地址 (例如: api.dify.ai): " domain
        read -p "使用HTTPS? (y/n, 默认y): " https
        https=${https:-y}
        if [ "$https" = "y" ]; then
            DIFY_URL="https://${domain}/chatbot/DNGyJpSDOa9BsC0H"
        else
            DIFY_URL="http://${domain}/chatbot/DNGyJpSDOa9BsC0H"
        fi
        ;;
    4)
        echo ""
        read -p "请输入完整的Dify URL: " DIFY_URL
        ;;
    *)
        echo "无效选择"
        exit 1
        ;;
esac

echo ""
echo "配置的Dify URL: $DIFY_URL"
read -p "确认使用此地址? (y/n): " confirm

if [ "$confirm" != "y" ]; then
    echo "已取消"
    exit 0
fi

# 备份原配置
if [ -f ".env" ]; then
    cp .env .env.bak
    echo "✅ 已备份原配置到 .env.bak"
fi

# 更新配置
if grep -q "^DIFY_CHATBOT_URL" .env; then
    # 如果已存在，替换
    sed -i "s|^DIFY_CHATBOT_URL=.*|DIFY_CHATBOT_URL=${DIFY_URL}|" .env
else
    # 如果不存在，添加
    echo "DIFY_CHATBOT_URL=${DIFY_URL}" >> .env
fi

echo ""
echo "✅ 配置已更新"
echo ""

# 测试连接
echo "测试Dify服务连接..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "$DIFY_URL" 2>/dev/null)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ] || [ "$HTTP_CODE" = "301" ]; then
    echo "✅ Dify服务可访问 (HTTP $HTTP_CODE)"
else
    echo "⚠️  Dify服务无法访问 (HTTP $HTTP_CODE)"
    echo "   请确认："
    echo "   1. Dify服务是否运行"
    echo "   2. 地址是否正确"
    echo "   3. 防火墙是否开放端口"
fi

echo ""
echo "下一步："
echo "1. 重启服务: sudo systemctl restart game-app"
echo "2. 验证: 访问 http://123.57.59.156:3000/ai-study?team_id=你的团队ID"
echo ""

