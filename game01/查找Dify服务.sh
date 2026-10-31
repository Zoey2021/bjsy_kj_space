#!/bin/bash
# 查找Dify服务实际地址

echo "=========================================="
echo "查找Dify服务"
echo "=========================================="
echo ""

# 1. 检查Dify相关进程
echo "【1】检查Dify相关进程"
echo "----------------------------------------"
ps aux | grep -i dify | grep -v grep || echo "未找到Dify进程"
echo ""

# 2. 检查Docker容器（如果使用Docker）
echo "【2】检查Docker容器"
echo "----------------------------------------"
if command -v docker &> /dev/null; then
    docker ps | grep -i dify || echo "未找到Dify容器"
else
    echo "Docker未安装"
fi
echo ""

# 3. 检查常见端口
echo "【3】检查常见端口"
echo "----------------------------------------"
echo "检查端口5000（Dify默认端口）："
netstat -tlnp | grep ":5000 " || echo "  端口5000未监听"
echo ""
echo "检查端口8000："
netstat -tlnp | grep ":8000 " || echo "  端口8000未监听"
echo ""
echo "检查端口3001："
netstat -tlnp | grep ":3001 " || echo "  端口3001未监听"
echo ""

# 4. 检查nginx配置
echo "【4】检查nginx配置"
echo "----------------------------------------"
if [ -d "/www/server/panel/vhost/nginx" ]; then
    echo "查找包含chatbot或dify的nginx配置："
    grep -r "chatbot\|dify" /www/server/panel/vhost/nginx/ 2>/dev/null | head -10 || echo "  未找到相关配置"
else
    echo "nginx配置目录不存在"
fi
echo ""

# 5. 检查宝塔网站配置
echo "【5】检查宝塔网站配置"
echo "----------------------------------------"
if [ -d "/www/server/panel/vhost" ]; then
    echo "网站列表："
    ls -la /www/server/panel/vhost/ 2>/dev/null | head -10
else
    echo "网站目录不存在"
fi
echo ""

# 6. 测试常见Dify路径
echo "【6】测试常见Dify路径"
echo "----------------------------------------"
TEST_URLS=(
    "http://localhost:5000"
    "http://localhost:8000"
    "http://localhost:3001"
    "http://localhost/chat"
    "http://localhost/api"
    "http://127.0.0.1:5000"
)

for URL in "${TEST_URLS[@]}"; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 "$URL" 2>/dev/null)
    if [ "$HTTP_CODE" != "000" ] && [ "$HTTP_CODE" != "" ]; then
        echo "✅ $URL - HTTP $HTTP_CODE"
    fi
done
echo ""

# 7. 检查环境变量
echo "【7】检查环境变量"
echo "----------------------------------------"
env | grep -i dify || echo "未找到DIFY相关环境变量"
echo ""

echo "=========================================="
echo "检查完成"
echo "=========================================="
echo ""
echo "📝 根据检查结果："
echo ""
echo "如果找到Dify服务："
echo "  1. 记录Dify服务的实际地址和端口"
echo "  2. 修改.env文件中的DIFY_CHATBOT_URL"
echo ""
echo "如果未找到Dify服务："
echo "  1. 确认Dify是否已部署"
echo "  2. 确认Dify服务的访问地址"
echo "  3. 检查nginx反向代理配置"
echo ""

