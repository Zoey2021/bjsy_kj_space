#!/bin/bash

echo "=========================================="
echo "验证 Dify 配置"
echo "=========================================="
echo ""

cd /www/wwwroot/game01

echo "【1】检查 .env 文件内容"
echo "----------------------------------------"
if [ -f ".env" ]; then
    echo "✅ .env 文件存在"
    echo ""
    echo "DIFY_CHATBOT_URL 配置:"
    grep "DIFY_CHATBOT_URL" .env || echo "❌ 未找到 DIFY_CHATBOT_URL"
    echo ""
else
    echo "❌ .env 文件不存在"
    exit 1
fi

echo "【2】测试 Python 读取环境变量"
echo "----------------------------------------"
# 确定虚拟环境路径
if [ -d "venv" ]; then
    VENV_PATH="venv"
elif [ -d ".venv" ]; then
    VENV_PATH=".venv"
else
    echo "❌ 未找到虚拟环境"
    exit 1
fi

source $VENV_PATH/bin/activate

python3 << 'PYTHON_SCRIPT'
import os
import sys

# 尝试加载 .env 文件
env_path = os.path.join(os.getcwd(), '.env')
if os.path.exists(env_path):
    try:
        from dotenv import load_dotenv
        load_dotenv(env_path)
        print("✅ 使用 python-dotenv 加载 .env 文件")
    except ImportError:
        # 手动读取
        print("⚠️  python-dotenv 未安装，手动读取 .env 文件")
        with open(env_path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                if '=' in line:
                    key, value = line.split('=', 1)
                    key = key.strip()
                    value = value.strip().strip('"').strip("'")
                    if key and key not in os.environ:
                        os.environ[key] = value
        print("✅ 手动加载 .env 文件完成")
else:
    print("❌ .env 文件不存在")

# 读取 DIFY_CHATBOT_URL
dify_url = os.getenv('DIFY_CHATBOT_URL')
if dify_url:
    print(f"\n✅ 成功读取 DIFY_CHATBOT_URL: {dify_url}")
    if 'localhost' in dify_url:
        print("⚠️  警告：URL 中包含 localhost，其他电脑无法访问")
    elif 'ngrok' in dify_url or 'ngrok-free' in dify_url:
        print("✅ URL 包含 ngrok，应该可以正常访问")
    else:
        print("ℹ️  URL 格式正常")
else:
    print("\n❌ 未读取到 DIFY_CHATBOT_URL")
    sys.exit(1)
PYTHON_SCRIPT

echo ""
echo "【3】检查服务日志中的 Dify URL"
echo "----------------------------------------"
echo "查找最近的 Dify URL 输出..."
sudo journalctl -u game-app -n 100 --no-pager | grep -E "Dify|dify|当前使用的|iframe" -i | tail -10

echo ""
echo "【4】检查应用启动时的输出"
echo "----------------------------------------"
echo "查看完整的启动日志（最后50行）..."
sudo journalctl -u game-app -n 50 --no-pager | tail -30

echo ""
echo "=========================================="
echo "验证完成"
echo "=========================================="
echo ""
echo "如果看到正确的 ngrok URL（不包含 localhost），"
echo "请在其他电脑上测试访问 AI 学习助手页面。"
echo ""

