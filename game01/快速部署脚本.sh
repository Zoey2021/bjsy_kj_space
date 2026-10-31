#!/bin/bash
# 宝塔面板快速部署脚本
# 服务器IP: 123.57.59.156
# 端口: 3000

set -e

echo "=========================================="
echo "开始部署 Python+AI小游戏设计课程应用"
echo "服务器: 123.57.59.156:3000"
echo "=========================================="

# 项目目录
PROJECT_DIR="/www/wwwroot/game01"
VENV_DIR="$PROJECT_DIR/.venv"

# 检查是否在项目目录
if [ ! -f "$PROJECT_DIR/app.py" ]; then
    echo "❌ 错误: 请在项目目录执行此脚本"
    echo "   当前目录: $(pwd)"
    echo "   项目目录应该是: $PROJECT_DIR"
    exit 1
fi

cd $PROJECT_DIR

# 1. 检查Python版本
echo ""
echo "📋 步骤1: 检查Python版本..."
python3 --version || {
    echo "❌ Python3 未安装，请先安装Python3"
    exit 1
}

# 2. 创建虚拟环境
echo ""
echo "📋 步骤2: 创建Python虚拟环境..."
if [ ! -d "$VENV_DIR" ]; then
    python3 -m venv .venv
    echo "✅ 虚拟环境创建成功"
else
    echo "✅ 虚拟环境已存在"
fi

# 3. 激活虚拟环境并安装依赖
echo ""
echo "📋 步骤3: 安装Python依赖..."
source .venv/bin/activate
pip install --upgrade pip -q
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

echo "✅ 依赖安装完成"

# 4. 创建必要目录
echo ""
echo "📋 步骤4: 创建必要目录..."
mkdir -p data/conversations
mkdir -p logs
echo "✅ 目录创建完成"

# 5. 设置权限
echo ""
echo "📋 步骤5: 设置文件权限..."
chown -R www:www $PROJECT_DIR
chmod -R 755 $PROJECT_DIR
chmod -R 775 $PROJECT_DIR/data
chmod -R 775 $PROJECT_DIR/logs
echo "✅ 权限设置完成"

# 6. 检查必要文件
echo ""
echo "📋 步骤6: 检查必要文件..."

if [ ! -f "$PROJECT_DIR/.env" ]; then
    echo "⚠️  警告: .env 文件不存在"
    echo "   请创建 .env 文件并配置环境变量"
    echo "   参考: 宝塔部署完整指南.md 第五步"
fi

if [ ! -f "$PROJECT_DIR/students_export.csv" ]; then
    echo "⚠️  警告: students_export.csv 文件不存在"
    echo "   请上传学生数据文件"
fi

if [ ! -f "$PROJECT_DIR/private_key.pem" ]; then
    echo "⚠️  警告: private_key.pem 文件不存在（如果使用JWT需要此文件）"
fi

# 7. 配置Systemd服务
echo ""
echo "📋 步骤7: 配置Systemd服务..."
if [ -f "$PROJECT_DIR/game-app.service" ]; then
    sudo cp $PROJECT_DIR/game-app.service /etc/systemd/system/
    sudo systemctl daemon-reload
    echo "✅ 服务文件已安装"
else
    echo "⚠️  警告: game-app.service 文件不存在"
fi

# 8. 检查服务状态
echo ""
echo "📋 步骤8: 检查服务状态..."
if systemctl is-active --quiet game-app 2>/dev/null; then
    echo "✅ 服务正在运行"
    sudo systemctl status game-app --no-pager -l
else
    echo "ℹ️  服务未运行，请执行以下命令启动："
    echo "   sudo systemctl start game-app"
    echo "   sudo systemctl enable game-app"
fi

# 9. 检查端口
echo ""
echo "📋 步骤9: 检查端口3000..."
if netstat -tlnp 2>/dev/null | grep -q ":3000 "; then
    echo "✅ 端口3000正在监听"
    netstat -tlnp | grep ":3000 "
else
    echo "⚠️  端口3000未监听，请检查服务是否启动"
fi

echo ""
echo "=========================================="
echo "部署脚本执行完成！"
echo "=========================================="
echo ""
echo "📝 下一步操作："
echo ""
echo "1. 配置环境变量："
echo "   编辑文件: $PROJECT_DIR/.env"
echo "   参考: 宝塔部署完整指南.md 第五步"
echo ""
echo "2. 启动服务："
echo "   sudo systemctl start game-app"
echo "   sudo systemctl enable game-app"
echo ""
echo "3. 检查服务状态："
echo "   sudo systemctl status game-app"
echo ""
echo "4. 开放防火墙端口："
echo "   在宝塔面板 -> 安全 -> 防火墙"
echo "   添加端口: 3000"
echo ""
echo "5. 访问测试："
echo "   http://123.57.59.156:3000"
echo ""
echo "6. 查看日志："
echo "   sudo journalctl -u game-app -f"
echo "   或: tail -f $PROJECT_DIR/logs/gunicorn_error.log"
echo ""
echo "=========================================="

