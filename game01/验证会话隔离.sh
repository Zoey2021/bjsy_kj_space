#!/bin/bash

echo "=========================================="
echo "验证会话隔离功能"
echo "=========================================="
echo ""

cd /www/wwwroot/game01

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

echo "【1】检查两个团队的访问码"
echo "----------------------------------------"
python3 << 'PYTHON_SCRIPT'
from app import app, Team

with app.app_context():
    team1_id = 1766385151383
    team2_id = 1766385157800
    
    team1 = Team.query.filter_by(id=team1_id).first()
    team2 = Team.query.filter_by(id=team2_id).first()
    
    if team1:
        print(f"✅ 团队1 (ID: {team1.id})")
        print(f"   团队名称: {team1.team_name}")
        print(f"   班级: {team1.class_name}")
        print(f"   访问码: {team1.access_code}")
        print(f"   Dify user参数: {team1.access_code}")
    else:
        print(f"❌ 未找到团队1 (ID: {team1_id})")
    
    print()
    
    if team2:
        print(f"✅ 团队2 (ID: {team2.id})")
        print(f"   团队名称: {team2.team_name}")
        print(f"   班级: {team2.class_name}")
        print(f"   访问码: {team2.access_code}")
        print(f"   Dify user参数: {team2.access_code}")
    else:
        print(f"❌ 未找到团队2 (ID: {team2_id})")
    
    print()
    
    if team1 and team2:
        if team1.access_code and team2.access_code:
            if team1.access_code != team2.access_code:
                print("✅ 两个团队的访问码不同，会话应该隔离")
            else:
                print("❌ 两个团队的访问码相同！会话无法隔离")
        else:
            print("⚠️  有团队缺少访问码，正在检查...")
            if not team1.access_code:
                print(f"   团队1缺少访问码，需要生成")
            if not team2.access_code:
                print(f"   团队2缺少访问码，需要生成")
PYTHON_SCRIPT

echo ""
echo "【2】检查所有团队的访问码唯一性"
echo "----------------------------------------"
python3 << 'PYTHON_SCRIPT'
from app import app, Team
from collections import Counter

with app.app_context():
    teams = Team.query.all()
    access_codes = [t.access_code for t in teams if t.access_code]
    
    # 检查重复
    code_counts = Counter(access_codes)
    duplicates = {code: count for code, count in code_counts.items() if count > 1}
    
    if duplicates:
        print(f"❌ 发现重复的访问码:")
        for code, count in duplicates.items():
            print(f"   {code}: 被 {count} 个团队使用")
    else:
        print(f"✅ 所有 {len(access_codes)} 个访问码都是唯一的")
    
    # 检查是否有团队缺少访问码
    teams_without_code = [t for t in teams if not t.access_code]
    if teams_without_code:
        print(f"\n⚠️  有 {len(teams_without_code)} 个团队缺少访问码:")
        for team in teams_without_code[:10]:  # 只显示前10个
            print(f"   团队ID: {team.id}, 名称: {team.team_name}, 班级: {team.class_name}")
        if len(teams_without_code) > 10:
            print(f"   ... 还有 {len(teams_without_code) - 10} 个团队")
    else:
        print(f"\n✅ 所有团队都有访问码")
PYTHON_SCRIPT

echo ""
echo "【3】生成测试 URL"
echo "----------------------------------------"
python3 << 'PYTHON_SCRIPT'
from app import app, Team
import os

# 读取 Dify 基础 URL
env_file = os.path.join(os.getcwd(), '.env')
base_url = "http://localhost/chat/DNGyJpSDOa9BsC0H"
if os.path.exists(env_file):
    with open(env_file, 'r', encoding='utf-8') as f:
        for line in f:
            if 'DIFY_CHATBOT_URL=' in line:
                base_url = line.split('=', 1)[1].strip().strip('"').strip("'")
                break

with app.app_context():
    team1_id = 1766385151383
    team2_id = 1766385157800
    
    team1 = Team.query.filter_by(id=team1_id).first()
    team2 = Team.query.filter_by(id=team2_id).first()
    
    print("测试 URL（用于验证会话隔离）:")
    print()
    
    if team1 and team1.access_code:
        separator = "&" if "?" in base_url else "?"
        url1 = f"{base_url}{separator}user={team1.access_code}"
        print(f"团队1 AI学习助手:")
        print(f"  http://123.57.59.156:3000/ai-study?team_id={team1_id}")
        print(f"  Dify iframe URL: {url1}")
        print()
    
    if team2 and team2.access_code:
        separator = "&" if "?" in base_url else "?"
        url2 = f"{base_url}{separator}user={team2.access_code}"
        print(f"团队2 AI学习助手:")
        print(f"  http://123.57.59.156:3000/ai-study?team_id={team2_id}")
        print(f"  Dify iframe URL: {url2}")
        print()
    
    if team1 and team2 and team1.access_code and team2.access_code:
        if team1.access_code != team2.access_code:
            print("✅ 两个团队的 user 参数不同，会话应该隔离")
        else:
            print("❌ 两个团队的 user 参数相同，会话无法隔离")
PYTHON_SCRIPT

echo ""
echo "=========================================="
echo "验证完成"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 如果发现团队缺少访问码，访问 AI 学习助手页面会自动生成"
echo "2. 使用上面的测试 URL 验证会话隔离"
echo "3. 在不同团队的 AI 学习助手中发送消息，确认看不到其他团队的对话"
echo ""

