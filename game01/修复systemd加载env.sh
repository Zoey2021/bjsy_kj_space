#!/bin/bash

echo "=========================================="
echo "修复systemd服务加载.env文件"
echo "=========================================="
echo ""

SERVICE_FILE="/etc/systemd/system/game-app.service"
PROJECT_DIR="/www/wwwroot/game01"
ENV_FILE="${PROJECT_DIR}/.env"

# 检查服务文件是否存在
if [ ! -f "$SERVICE_FILE" ]; then
    echo "❌ 服务文件不存在: $SERVICE_FILE"
    exit 1
fi

# 备份服务文件
echo "✅ 备份服务文件..."
cp "$SERVICE_FILE" "${SERVICE_FILE}.backup.$(date +%Y%m%d_%H%M%S)"

# 检查是否已经有EnvironmentFile
if grep -q "EnvironmentFile" "$SERVICE_FILE"; then
    echo "⚠️  服务文件中已有EnvironmentFile配置"
    echo "当前配置:"
    grep "EnvironmentFile" "$SERVICE_FILE"
    echo ""
    read -p "是否更新为正确的路径？(y/n): " UPDATE
    if [ "$UPDATE" != "y" ] && [ "$UPDATE" != "Y" ]; then
        echo "已取消"
        exit 0
    fi
    # 删除旧的EnvironmentFile行
    sed -i '/EnvironmentFile=/d' "$SERVICE_FILE"
fi

# 在[Service]部分添加EnvironmentFile
echo "✅ 添加EnvironmentFile配置..."

# 检查[Service]部分是否存在
if ! grep -q "\[Service\]" "$SERVICE_FILE"; then
    echo "❌ 服务文件中没有[Service]部分"
    exit 1
fi

# 在[Service]部分后添加EnvironmentFile（在ExecStart之前）
sed -i '/\[Service\]/a EnvironmentFile='"${ENV_FILE}" "$SERVICE_FILE"

echo ""
echo "=========================================="
echo "更新后的服务配置"
echo "=========================================="
cat "$SERVICE_FILE"
echo ""

# 重新加载systemd配置
echo "=========================================="
echo "重新加载systemd配置"
echo "=========================================="
systemctl daemon-reload

# 重启服务
echo ""
read -p "是否立即重启服务？(y/n): " RESTART
if [ "$RESTART" = "y" ] || [ "$RESTART" = "Y" ]; then
    echo "正在重启服务..."
    systemctl restart game-app
    sleep 3
    echo ""
    echo "服务状态:"
    systemctl status game-app --no-pager | head -15
    echo ""
    echo "=========================================="
    echo "验证环境变量"
    echo "=========================================="
    echo "检查服务是否读取了环境变量:"
    systemctl show game-app | grep -i "environment\|dify" || echo "未找到Dify相关环境变量（这是正常的，systemd show不显示EnvironmentFile）"
    echo ""
    echo "查看最新日志，确认Dify URL:"
    journalctl -u game-app -n 10 --no-pager | grep -i "dify iframe url" || echo "等待新的请求..."
fi

echo ""
echo "=========================================="
echo "完成"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 在其他电脑上测试访问AI学习助手"
echo "2. 查看服务日志确认URL: sudo journalctl -u game-app -n 20 --no-pager | grep 'Dify iframe URL'"
echo ""

