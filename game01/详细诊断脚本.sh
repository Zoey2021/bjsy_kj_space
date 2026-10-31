#!/bin/bash
# 详细诊断脚本 - 找出无法访问的原因

echo "=========================================="
echo "详细诊断 - 无法访问 123.57.59.156:3000"
echo "=========================================="
echo ""

PROJECT_DIR="/www/wwwroot/game01"
cd $PROJECT_DIR

# 1. 检查服务状态
echo "【1】检查systemd服务状态"
echo "----------------------------------------"
sudo systemctl status game-app --no-pager -l | head -20
echo ""

# 2. 检查进程
echo "【2】检查Gunicorn进程"
echo "----------------------------------------"
ps aux | grep gunicorn | grep -v grep || echo "❌ 没有找到Gunicorn进程"
echo ""

# 3. 检查端口监听
echo "【3】检查端口3000是否监听"
echo "----------------------------------------"
if netstat -tlnp 2>/dev/null | grep -q ":3000 "; then
    echo "✅ 端口3000正在监听："
    netstat -tlnp | grep ":3000 "
else
    echo "❌ 端口3000未监听"
    echo ""
    echo "检查是否有其他进程占用："
    lsof -i:3000 2>/dev/null || echo "   端口3000未被占用"
fi
echo ""

# 4. 检查本地访问
echo "【4】测试本地访问"
echo "----------------------------------------"
if curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:3000 2>/dev/null | grep -q "200\|302\|301"; then
    echo "✅ 本地访问成功"
    curl -s http://127.0.0.1:3000 | head -5
else
    echo "❌ 本地访问失败"
    echo "   尝试连接："
    timeout 3 curl -v http://127.0.0.1:3000 2>&1 | head -10
fi
echo ""

# 5. 检查防火墙
echo "【5】检查防火墙规则"
echo "----------------------------------------"
if command -v firewall-cmd &> /dev/null; then
    echo "firewall-cmd 状态："
    firewall-cmd --list-ports 2>/dev/null | grep 3000 && echo "✅ 防火墙已开放3000端口" || echo "❌ 防火墙未开放3000端口"
elif command -v iptables &> /dev/null; then
    echo "iptables 规则："
    iptables -L -n | grep 3000 && echo "✅ iptables规则存在" || echo "⚠️  未找到iptables规则（可能使用宝塔防火墙）"
else
    echo "⚠️  无法检测防火墙（请手动检查宝塔防火墙）"
fi
echo ""

# 6. 检查服务日志
echo "【6】最近的服务日志（最后20行）"
echo "----------------------------------------"
sudo journalctl -u game-app -n 20 --no-pager
echo ""

# 7. 检查应用日志
echo "【7】应用错误日志"
echo "----------------------------------------"
if [ -f "$PROJECT_DIR/logs/gunicorn_error.log" ]; then
    echo "最近错误："
    tail -20 $PROJECT_DIR/logs/gunicorn_error.log
else
    echo "❌ 错误日志文件不存在"
fi
echo ""

# 8. 检查文件权限
echo "【8】检查关键文件权限"
echo "----------------------------------------"
ls -la app.py gunicorn_config.py game-app.service 2>/dev/null
echo ""

# 9. 检查虚拟环境
echo "【9】检查虚拟环境和依赖"
echo "----------------------------------------"
if [ -d ".venv" ]; then
    echo "✅ 虚拟环境存在"
    source .venv/bin/activate
    echo "Python版本: $(python --version 2>&1)"
    echo "Gunicorn版本: $(pip show gunicorn 2>/dev/null | grep Version || echo '未安装')"
    echo "Flask版本: $(pip show Flask 2>/dev/null | grep Version || echo '未安装')"
else
    echo "❌ 虚拟环境不存在"
fi
echo ""

# 10. 检查配置文件
echo "【10】检查Gunicorn配置"
echo "----------------------------------------"
if [ -f "gunicorn_config.py" ]; then
    echo "配置文件内容："
    grep -E "bind|workers|port" gunicorn_config.py
else
    echo "❌ gunicorn_config.py 不存在"
fi
echo ""

# 11. 尝试手动启动测试
echo "【11】手动启动测试（5秒后自动停止）"
echo "----------------------------------------"
echo "如果服务未运行，尝试手动启动："
if [ -d ".venv" ]; then
    source .venv/bin/activate
    echo "执行: gunicorn -c gunicorn_config.py app:app --daemon"
    echo "（这将以后台方式启动，用于测试）"
    echo ""
    echo "⚠️  注意：如果服务已经在运行，请先停止："
    echo "   sudo systemctl stop game-app"
fi
echo ""

# 12. 网络连接测试
echo "【12】网络连接测试"
echo "----------------------------------------"
echo "测试外网IP连接："
if timeout 3 curl -s -o /dev/null -w "HTTP状态码: %{http_code}\n" http://123.57.59.156:3000 2>&1; then
    echo "✅ 外网可以访问"
else
    echo "❌ 外网无法访问"
    echo "   可能原因："
    echo "   1. 防火墙未开放端口"
    echo "   2. 云服务器安全组未开放端口"
    echo "   3. 服务未监听0.0.0.0（只监听了127.0.0.1）"
fi
echo ""

echo "=========================================="
echo "诊断完成"
echo "=========================================="
echo ""
echo "📝 根据以上诊断结果："
echo ""
echo "如果服务未运行："
echo "   sudo systemctl start game-app"
echo ""
echo "如果端口未监听："
echo "   检查服务是否启动，查看日志："
echo "   sudo journalctl -u game-app -n 50"
echo ""
echo "如果本地可以访问但外网不行："
echo "   1. 检查宝塔防火墙（安全 → 防火墙）"
echo "   2. 检查云服务器安全组（必须！）"
echo ""
echo "如果服务启动失败："
echo "   查看详细错误："
echo "   sudo journalctl -u game-app -n 100"
echo ""

