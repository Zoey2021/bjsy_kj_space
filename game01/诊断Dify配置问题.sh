#!/bin/bash

echo "=========================================="
echo "诊断Dify配置问题"
echo "=========================================="
echo ""

cd /www/wwwroot/game01

echo "【1】检查.env文件中的配置"
echo "----------------------------------------"
if [ -f ".env" ]; then
    echo "✅ .env文件存在"
    echo ""
    echo "当前DIFY_CHATBOT_URL配置:"
    grep DIFY_CHATBOT_URL .env || echo "❌ 未找到DIFY_CHATBOT_URL配置"
    echo ""
    
    # 检查是否包含localhost
    if grep -q "localhost" .env 2>/dev/null; then
        echo "⚠️  警告：配置中包含localhost！"
        grep "localhost" .env
    else
        echo "✅ 配置中没有localhost"
    fi
else
    echo "❌ .env文件不存在"
fi

echo ""
echo "【2】检查环境变量（当前进程）"
echo "----------------------------------------"
# 检查systemd服务中的环境变量
if systemctl is-active --quiet game-app; then
    echo "✅ 服务正在运行"
    echo ""
    echo "服务环境变量:"
    systemctl show game-app | grep -i "environment\|dify" || echo "未找到Dify相关环境变量"
else
    echo "❌ 服务未运行"
fi

echo ""
echo "【3】检查服务日志中的Dify URL"
echo "----------------------------------------"
echo "最近50行日志中关于Dify的信息:"
journalctl -u game-app -n 50 --no-pager | grep -i "dify\|localhost" || echo "未找到相关信息"

echo ""
echo "【4】测试环境变量读取"
echo "----------------------------------------"
# 尝试从.env文件读取并显示
if [ -f ".env" ]; then
    source .env 2>/dev/null
    if [ -n "$DIFY_CHATBOT_URL" ]; then
        echo "✅ 环境变量可以读取"
        echo "DIFY_CHATBOT_URL=$DIFY_CHATBOT_URL"
        
        # 检查是否包含localhost
        if [[ "$DIFY_CHATBOT_URL" == *"localhost"* ]]; then
            echo ""
            echo "❌ 问题发现：URL中包含localhost！"
            echo "   需要修改为ngrok地址"
        else
            echo ""
            echo "✅ URL不包含localhost"
        fi
    else
        echo "❌ 环境变量未设置"
    fi
fi

echo ""
echo "【5】检查URL完整性"
echo "----------------------------------------"
if [ -f ".env" ]; then
    URL=$(grep "^DIFY_CHATBOT_URL=" .env | cut -d'=' -f2- | tr -d '"' | tr -d "'")
    if [ -n "$URL" ]; then
        echo "当前URL: $URL"
        echo ""
        
        # 检查URL长度（完整URL应该比较长）
        URL_LENGTH=${#URL}
        echo "URL长度: $URL_LENGTH 字符"
        
        if [ "$URL_LENGTH" -lt 50 ]; then
            echo "⚠️  警告：URL可能不完整（通常应该超过50字符）"
        fi
        
        # 检查是否包含ngrok
        if [[ "$URL" == *"ngrok"* ]]; then
            echo "✅ URL包含ngrok"
        else
            echo "❌ URL不包含ngrok"
        fi
        
        # 检查是否包含完整的chatbot ID
        if [[ "$URL" == *"DNGyJpSDOa9BsC0H"* ]]; then
            echo "✅ URL包含完整的chatbot ID"
        else
            echo "⚠️  警告：URL可能不包含完整的chatbot ID"
            echo "   应该是: DNGyJpSDOa9BsC0H"
        fi
    fi
fi

echo ""
echo "=========================================="
echo "诊断完成"
echo "=========================================="
echo ""
echo "如果发现问题，请："
echo "1. 检查.env文件中的DIFY_CHATBOT_URL是否完整"
echo "2. 确保URL不包含localhost"
echo "3. 确保URL包含完整的ngrok地址和chatbot ID"
echo "4. 重启服务: sudo systemctl restart game-app"
echo ""

