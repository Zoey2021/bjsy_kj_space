#!/bin/bash
# 检查Dify配置脚本

cd /www/wwwroot/game01

echo "=========================================="
echo "Dify智能体配置检查"
echo "=========================================="
echo ""

# 1. 检查.env配置
echo "【1】检查.env配置"
echo "----------------------------------------"
if [ -f ".env" ]; then
    DIFY_URL=$(grep "^DIFY_CHATBOT_URL" .env | cut -d'=' -f2 | tr -d '"' | tr -d "'")
    if [ -n "$DIFY_URL" ]; then
        echo "✅ 找到配置: DIFY_CHATBOT_URL=$DIFY_URL"
    else
        echo "❌ 未找到DIFY_CHATBOT_URL配置"
        echo "   请在.env文件中添加："
        echo "   DIFY_CHATBOT_URL=http://localhost/chatbot/DNGyJpSDOa9BsC0H"
    fi
else
    echo "❌ .env文件不存在"
fi
echo ""

# 2. 测试Dify服务
echo "【2】测试Dify服务可访问性"
echo "----------------------------------------"
if [ -n "$DIFY_URL" ]; then
    echo "测试URL: $DIFY_URL"
    echo ""
    
    # 测试HTTP连接
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "$DIFY_URL" 2>/dev/null)
    
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ] || [ "$HTTP_CODE" = "301" ]; then
        echo "✅ Dify服务可访问 (HTTP $HTTP_CODE)"
    else
        echo "❌ Dify服务无法访问 (HTTP $HTTP_CODE)"
        echo ""
        echo "   详细错误："
        curl -v "$DIFY_URL" 2>&1 | head -20
        echo ""
        echo "   可能原因："
        echo "   1. Dify服务未运行"
        echo "   2. URL配置错误"
        echo "   3. 防火墙阻止访问"
    fi
else
    echo "⚠️  无法测试（未配置DIFY_CHATBOT_URL）"
fi
echo ""

# 3. 检查应用日志中的Dify相关错误
echo "【3】检查应用日志"
echo "----------------------------------------"
if [ -f "logs/gunicorn_error.log" ]; then
    DIFY_ERRORS=$(grep -i "dify\|iframe\|chatbot" logs/gunicorn_error.log | tail -5)
    if [ -n "$DIFY_ERRORS" ]; then
        echo "找到相关错误："
        echo "$DIFY_ERRORS"
    else
        echo "✅ 未找到Dify相关错误"
    fi
else
    echo "⚠️  日志文件不存在"
fi
echo ""

# 4. 检查Dify服务是否运行（如果使用localhost）
echo "【4】检查本地Dify服务"
echo "----------------------------------------"
if echo "$DIFY_URL" | grep -q "localhost\|127.0.0.1"; then
    echo "检测到使用localhost，检查本地服务..."
    
    # 提取端口
    PORT=$(echo "$DIFY_URL" | sed -n 's/.*:\([0-9]*\).*/\1/p')
    if [ -z "$PORT" ]; then
        PORT="80"
    fi
    
    echo "检查端口 $PORT 是否监听..."
    if netstat -tlnp 2>/dev/null | grep -q ":$PORT "; then
        echo "✅ 端口 $PORT 正在监听"
        netstat -tlnp | grep ":$PORT "
    else
        echo "❌ 端口 $PORT 未监听"
        echo "   Dify服务可能未运行"
    fi
else
    echo "使用外部Dify服务，跳过本地检查"
fi
echo ""

# 5. 提供修复建议
echo "=========================================="
echo "修复建议"
echo "=========================================="
echo ""

if [ -z "$DIFY_URL" ]; then
    echo "1. 配置DIFY_CHATBOT_URL："
    echo "   cd /www/wwwroot/game01"
    echo "   nano .env"
    echo "   添加：DIFY_CHATBOT_URL=http://你的Dify地址/chatbot/DNGyJpSDOa9BsC0H"
    echo ""
fi

if [ "$HTTP_CODE" != "200" ] && [ "$HTTP_CODE" != "302" ] && [ "$HTTP_CODE" != "301" ]; then
    echo "2. 检查Dify服务："
    echo "   - 确认Dify服务是否运行"
    echo "   - 确认Dify服务地址是否正确"
    echo "   - 测试访问：curl -I $DIFY_URL"
    echo ""
fi

echo "3. 重启服务："
echo "   sudo systemctl restart game-app"
echo ""
echo "4. 验证："
echo "   访问 http://123.57.59.156:3000/ai-study?team_id=你的团队ID"
echo "   检查左侧AI学习助手是否正常加载"
echo ""

