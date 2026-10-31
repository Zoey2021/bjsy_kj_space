#!/bin/bash
# 一键诊断和修复脚本

set -e

PROJECT_DIR="/www/wwwroot/game01"
cd $PROJECT_DIR

echo "=========================================="
echo "开始诊断和修复"
echo "=========================================="
echo ""

# 诊断
echo "📋 诊断信息："
echo ""

echo "1. 检查文件..."
if [ -f "app.py" ]; then
    echo "   ✅ app.py 存在"
else
    echo "   ❌ app.py 不存在"
    exit 1
fi

if [ -f "requirements.txt" ]; then
    echo "   ✅ requirements.txt 存在"
else
    echo "   ❌ requirements.txt 不存在"
    exit 1
fi

if [ -f "gunicorn_config.py" ]; then
    echo "   ✅ gunicorn_config.py 存在"
else
    echo "   ❌ gunicorn_config.py 不存在"
    exit 1
fi

if [ -f "game-app.service" ]; then
    echo "   ✅ game-app.service 存在"
else
    echo "   ❌ game-app.service 不存在"
    exit 1
fi
echo ""

echo "2. 检查虚拟环境..."
if [ -d ".venv" ]; then
    echo "   ✅ 虚拟环境存在"
    source .venv/bin/activate
    PYTHON_VERSION=$(python --version 2>&1)
    echo "   Python版本: $PYTHON_VERSION"
else
    echo "   ❌ 虚拟环境不存在，正在创建..."
    python3 -m venv .venv
    source .venv/bin/activate
    pip install --upgrade pip -q -i https://pypi.tuna.tsinghua.edu.cn/simple
fi
echo ""

echo "3. 修复requirements.txt（确保兼容Python 3.6）..."
cat > requirements.txt << 'EOF'
Flask==2.0.3
Flask-SQLAlchemy==2.5.1
PyJWT==2.4.0
cryptography==3.4.8
requests==2.27.1
python-docx==0.8.11
openpyxl==3.0.10
gunicorn==20.1.0
EOF
echo "   ✅ requirements.txt 已修复"
echo ""

echo "4. 安装/更新依赖..."
source .venv/bin/activate
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
echo "   ✅ 依赖安装完成"
echo ""

echo "5. 创建必要目录..."
mkdir -p data/conversations
mkdir -p logs
echo "   ✅ 目录创建完成"
echo ""

echo "6. 修复权限..."
chown -R www:www $PROJECT_DIR
chmod -R 755 $PROJECT_DIR
chmod -R 775 $PROJECT_DIR/data
chmod -R 775 $PROJECT_DIR/logs
echo "   ✅ 权限修复完成"
echo ""

echo "7. 创建.env文件（如果不存在）..."
if [ ! -f ".env" ]; then
    SECRET_KEY=$(python3 -c "import secrets; print(secrets.token_urlsafe(32))" 2>/dev/null || echo "change-this-secret-key-$(date +%s)")
    cat > .env << EOF
FLASK_SECRET_KEY=${SECRET_KEY}
PORT=3000
COZE_WEB_TOKEN=pat_D0tiRReTn3pT5Ottzs6gNQo9Y0b2WdnDP3KaBavHVJ4RhkA9xENr55XDmhS4tr4f
COZE_APP_ID=7583740783930032138
COZE_OAUTH_APP_ID=1137110341365
COZE_OAUTH_KID=mk8SZRnkzw0vpOZZ3WWzcFLz0ZID3s3s2TwqPVM7hlA
DIFY_CHATBOT_URL=http://localhost/chatbot/DNGyJpSDOa9BsC0H
EOF
    chown www:www .env
    chmod 600 .env
    echo "   ✅ .env 文件已创建"
else
    echo "   ✅ .env 文件已存在"
fi
echo ""

echo "8. 配置systemd服务..."
if [ -f "game-app.service" ]; then
    sudo cp game-app.service /etc/systemd/system/
    sudo systemctl daemon-reload
    echo "   ✅ 服务文件已安装"
else
    echo "   ❌ game-app.service 不存在"
    exit 1
fi
echo ""

echo "9. 启动服务..."
sudo systemctl stop game-app 2>/dev/null || true
sudo systemctl start game-app
sleep 2
sudo systemctl enable game-app
echo "   ✅ 服务已启动"
echo ""

echo "10. 检查服务状态..."
if sudo systemctl is-active --quiet game-app; then
    echo "   ✅ 服务正在运行"
else
    echo "   ❌ 服务未运行，查看错误："
    sudo systemctl status game-app --no-pager -l | head -20
    echo ""
    echo "   查看详细日志："
    sudo journalctl -u game-app -n 30 --no-pager
    exit 1
fi
echo ""

echo "11. 检查端口..."
if netstat -tlnp 2>/dev/null | grep -q ":3000 "; then
    echo "   ✅ 端口3000正在监听"
    netstat -tlnp | grep ":3000 "
else
    echo "   ⚠️  端口3000未监听，等待5秒后重试..."
    sleep 5
    if netstat -tlnp 2>/dev/null | grep -q ":3000 "; then
        echo "   ✅ 端口3000现在正在监听"
        netstat -tlnp | grep ":3000 "
    else
        echo "   ❌ 端口3000仍未监听"
        echo "   查看服务日志："
        sudo journalctl -u game-app -n 30 --no-pager
    fi
fi
echo ""

echo "=========================================="
echo "诊断和修复完成！"
echo "=========================================="
echo ""
echo "📝 下一步操作："
echo ""
echo "1. 检查防火墙："
echo "   宝塔面板 → 安全 → 防火墙 → 添加端口3000"
echo ""
echo "2. 检查云服务器安全组："
echo "   登录云服务器控制台 → 安全组 → 添加入站规则：端口3000"
echo ""
echo "3. 测试访问："
echo "   http://123.57.59.156:3000"
echo ""
echo "4. 查看服务状态："
echo "   sudo systemctl status game-app"
echo ""
echo "5. 查看日志："
echo "   sudo journalctl -u game-app -f"
echo "   或: tail -f /www/wwwroot/game01/logs/gunicorn_error.log"
echo ""
echo "=========================================="

