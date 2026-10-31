#!/bin/bash

echo "=========================================="
echo "服务器性能诊断"
echo "=========================================="
echo ""

echo "【1】系统资源使用情况"
echo "----------------------------------------"
echo "CPU核心数:"
nproc
echo ""

echo "CPU使用率:"
top -bn1 | grep "Cpu(s)" | sed "s/.*, *\([0-9.]*\)%* id.*/\1/" | awk '{print "使用率: " 100 - $1"%"}'
echo ""

echo "内存使用:"
free -h
echo ""

echo "磁盘使用:"
df -h / | tail -1
echo ""

echo "【2】网络带宽检查"
echo "----------------------------------------"
echo "当前网络接口:"
ip addr show | grep -E "^[0-9]+:" | grep -v lo
echo ""

echo "网络连接数:"
netstat -an | grep ESTABLISHED | wc -l
echo "总连接数:"
netstat -an | wc -l
echo ""

echo "【3】Gunicorn进程状态"
echo "----------------------------------------"
if pgrep -f gunicorn > /dev/null; then
    echo "✅ Gunicorn正在运行"
    echo ""
    echo "进程信息:"
    ps aux | grep gunicorn | grep -v grep | head -5
    echo ""
    echo "Worker数量:"
    ps aux | grep gunicorn | grep -v grep | wc -l
    echo ""
    echo "内存使用:"
    ps aux | grep gunicorn | grep -v grep | awk '{sum+=$6} END {print "总内存: " sum/1024 " MB"}'
else
    echo "❌ Gunicorn未运行"
fi
echo ""

echo "【4】数据库状态"
echo "----------------------------------------"
cd /www/wwwroot/game01 2>/dev/null || {
    echo "⚠️  无法切换到项目目录"
    exit 1
}

if [ -f "data/game_app"*.db ]; then
    DB_FILE=$(ls -t data/game_app*.db | head -1)
    echo "数据库文件: $DB_FILE"
    echo "文件大小:"
    ls -lh "$DB_FILE" | awk '{print $5}'
    echo ""
    
    # 检查数据库连接（需要Python环境）
    if [ -d "venv" ] || [ -d ".venv" ]; then
        VENV_PATH="venv"
        [ -d ".venv" ] && VENV_PATH=".venv"
        source $VENV_PATH/bin/activate 2>/dev/null
        
        python3 << 'PYTHON_SCRIPT'
import sqlite3
import os
import glob

db_files = glob.glob('data/game_app*.db')
if db_files:
    db_file = db_files[0]
    try:
        conn = sqlite3.connect(db_file, timeout=5)
        cursor = conn.cursor()
        
        # 检查表数量
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = cursor.fetchall()
        print(f"数据库表数量: {len(tables)}")
        
        # 检查团队数量
        cursor.execute("SELECT COUNT(*) FROM teams")
        team_count = cursor.fetchone()[0]
        print(f"团队数量: {team_count}")
        
        # 检查学生数量
        cursor.execute("SELECT COUNT(*) FROM students")
        student_count = cursor.fetchone()[0]
        print(f"学生数量: {student_count}")
        
        conn.close()
        print("✅ 数据库连接正常")
    except Exception as e:
        print(f"❌ 数据库连接失败: {e}")
PYTHON_SCRIPT
    fi
else
    echo "❌ 未找到数据库文件"
fi
echo ""

echo "【5】应用日志检查"
echo "----------------------------------------"
if [ -f "logs/gunicorn_access.log" ]; then
    echo "最近的访问日志（最后10条）:"
    tail -10 logs/gunicorn_access.log 2>/dev/null | head -5
    echo ""
    
    echo "响应时间统计（最近100条）:"
    tail -100 logs/gunicorn_access.log 2>/dev/null | grep -oE '[0-9]+\.[0-9]+$' | awk '{
        sum+=$1
        count++
        if ($1 > max) max=$1
        if (count==1 || $1 < min) min=$1
    } END {
        if (count > 0) {
            print "平均响应时间: " sum/count " 秒"
            print "最大响应时间: " max " 秒"
            print "最小响应时间: " min " 秒"
            print "请求总数: " count
        }
    }'
else
    echo "⚠️  访问日志文件不存在"
fi
echo ""

echo "【6】系统负载"
echo "----------------------------------------"
uptime
echo ""

echo "【7】带宽使用建议"
echo "----------------------------------------"
echo "如果10个团队同时使用，建议："
echo "1. 检查阿里云控制台的带宽使用情况"
echo "2. 如果带宽使用率 > 80%，考虑升级带宽"
echo "3. 建议带宽：5-10Mbps（10个团队同时使用）"
echo ""

echo "=========================================="
echo "诊断完成"
echo "=========================================="
echo ""
echo "优化建议："
echo "1. 如果CPU使用率 > 80%，考虑升级CPU"
echo "2. 如果内存使用率 > 80%，考虑增加内存"
echo "3. 如果带宽使用率 > 80%，考虑升级带宽"
echo "4. 如果数据库文件 > 100MB，考虑优化数据库"
echo ""


