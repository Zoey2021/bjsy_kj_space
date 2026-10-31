from flask import Flask, render_template, jsonify, request, session, redirect, url_for, send_file
import csv
import json
import os
import random
import re
import string
import time
import uuid
import jwt
import requests
from datetime import datetime

# 加载.env文件（如果存在）
env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), '.env')
try:
    from dotenv import load_dotenv
    # 加载项目根目录的.env文件
    if os.path.exists(env_path):
        load_dotenv(env_path)
        print(f"✅ 已加载.env文件（使用python-dotenv）: {env_path}")
    else:
        print(f"⚠️  .env文件不存在: {env_path}")
except ImportError:
    # 备用方案：手动读取.env文件
    print("⚠️  python-dotenv未安装，尝试手动读取.env文件")
    if os.path.exists(env_path):
        try:
            with open(env_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    # 跳过空行和注释
                    if not line or line.startswith('#'):
                        continue
                    # 解析 KEY=VALUE 格式
                    if '=' in line:
                        key, value = line.split('=', 1)
                        key = key.strip()
                        value = value.strip().strip('"').strip("'")
                        # 如果环境变量未设置，则设置它
                        if key and key not in os.environ:
                            os.environ[key] = value
            print(f"✅ 已手动加载.env文件: {env_path}")
        except Exception as e:
            print(f"⚠️  手动读取.env文件失败: {e}，将使用系统环境变量")
    else:
        print(f"⚠️  .env文件不存在: {env_path}，将使用系统环境变量")
from docx import Document
from docx.shared import Pt, RGBColor, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from io import BytesIO
from openpyxl import Workbook
from openpyxl.styles import Font, Alignment, PatternFill, Border, Side
from functools import lru_cache
from threading import Lock
# 尝试导入数据库模型，如果失败则使用文件存储
USE_DATABASE = True
try:
    from models import db, Student, Team, TeamMember, Conversation, ConversationMessage
    from sqlalchemy.orm import joinedload
    DB_AVAILABLE = True
except ImportError as e:
    print(f"⚠️  数据库模块导入失败，将使用文件存储: {e}")
    DB_AVAILABLE = False
    USE_DATABASE = False
    # 创建虚拟的 db 对象以避免后续错误
    class DummyDB:
        def init_app(self, app):
            pass
        def create_all(self):
            pass
    db = DummyDB()
    joinedload = None

app = Flask(__name__)
app.secret_key = os.environ.get("FLASK_SECRET_KEY", "python-ai-game-demo-secret")  # 开发环境默认值，生产请改

# 数据文件路径
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data")
TEAMS_FILE = os.path.join(DATA_DIR, "teams.json")
STUDENTS_CSV = os.path.join(BASE_DIR, "students_export.csv")
CONVERSATIONS_DIR = os.path.join(DATA_DIR, "conversations")

# 数据库配置（仅在数据库可用时）
if DB_AVAILABLE:
    # 优先使用 game_app-1227.db，如果不存在则使用 game_app.db
    db_path_1227 = os.path.join(DATA_DIR, "game_app-1227.db")
    db_path_default = os.path.join(DATA_DIR, "game_app.db")
    db_path = db_path_1227 if os.path.exists(db_path_1227) else db_path_default
    print(f"📁 数据库文件: {db_path}")
    print(f"   {'✅ 使用 game_app-1227.db' if db_path == db_path_1227 else '⚠️  使用 game_app.db (未找到 game_app-1227.db)'}")
    # SQLite 性能优化配置
    app.config['SQLALCHEMY_DATABASE_URI'] = f'sqlite:///{db_path}?check_same_thread=False'
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    app.config['SQLALCHEMY_ENGINE_OPTIONS'] = {
        'pool_pre_ping': True,  # 连接前检查连接是否有效
        'pool_recycle': 300,    # 连接回收时间（秒）
        'connect_args': {
            'check_same_thread': False,  # 允许多线程访问
            'timeout': 5,  # 查询超时时间（秒）- 减少到5秒，快速失败，避免长时间占用CPU
        }
    }
    # 初始化数据库
    db.init_app(app)
COZE_WEB_TOKEN = os.environ.get("COZE_WEB_TOKEN", "pat_D0tiRReTn3pT5Ottzs6gNQo9Y0b2WdnDP3KaBavHVJ4RhkA9xENr55XDmhS4tr4f")
COZE_APP_ID = os.environ.get("COZE_APP_ID", "7583740783930032138")

# OAuth JWT 配置（用于团队级会话隔离）
# 优先从环境变量读取，如果没有则使用默认值（从您提供的配置）
COZE_OAUTH_APP_ID = os.environ.get("COZE_OAUTH_APP_ID", "1137110341365")  # OAuth应用ID（iss）
COZE_OAUTH_PRIVATE_KEY = os.environ.get("COZE_OAUTH_PRIVATE_KEY", "")  # RSA私钥（PEM格式，优先从文件读取）
COZE_OAUTH_KID = os.environ.get("COZE_OAUTH_KID", "mk8SZRnkzw0vpOZZ3WWzcFLz0ZID3s3s2TwqPVM7hlA")  # 公钥指纹（kid）

# 确保数据目录存在
os.makedirs(DATA_DIR, exist_ok=True)
os.makedirs(CONVERSATIONS_DIR, exist_ok=True)

# 初始化数据库表（如果不存在）
if DB_AVAILABLE:
    try:
        with app.app_context():
            # 确保数据库文件目录存在
            db_dir = os.path.dirname(db_path)
            os.makedirs(db_dir, exist_ok=True)
            
            # 创建所有表
            db.create_all()
            
            # 验证数据库文件是否创建成功
            if os.path.exists(db_path):
                print(f"✅ 数据库初始化成功: {db_path}")
            else:
                print(f"⚠️  警告：数据库文件未创建: {db_path}")
                USE_DATABASE = False
    except Exception as e:
        print(f"⚠️  数据库初始化失败: {e}")
        import traceback
        traceback.print_exc()
        USE_DATABASE = False
else:
    USE_DATABASE = False


def load_students():
    """从数据库加载学生数据（如果数据库为空或不可用，则从CSV读取）"""
    # 如果数据库不可用，直接从CSV读取
    if not USE_DATABASE or not DB_AVAILABLE:
        if os.path.exists(STUDENTS_CSV):
            students = []
            class_names = set()
            with open(STUDENTS_CSV, newline="", encoding="utf-8") as csvfile:
                reader = csv.DictReader(csvfile)
                for row in reader:
                    student = {
                        "class_name": row["class_name"],
                        "student_id": int(row["student_id"]),
                        "name": row["name"],
                        "avatar": row.get("avatar", "👤"),
                    }
                    students.append(student)
                    class_names.add(row["class_name"])
            return students, sorted(class_names)
        return [], []
    
    # 使用数据库
    with app.app_context():
        try:
            # 先尝试从数据库读取
            db_students = Student.query.all()
            if db_students:
                students = [s.to_dict() for s in db_students]
                class_names = sorted(set(s["class_name"] for s in students))
                return students, class_names
        except Exception as e:
            print(f"⚠️  从数据库读取学生数据失败: {e}")
        
        # 如果数据库为空或出错，从CSV读取（用于首次迁移）
        if os.path.exists(STUDENTS_CSV):
            students = []
            class_names = set()
            with open(STUDENTS_CSV, newline="", encoding="utf-8") as csvfile:
                reader = csv.DictReader(csvfile)
                for row in reader:
                    student = {
                        "class_name": row["class_name"],
                        "student_id": int(row["student_id"]),
                        "name": row["name"],
                        "avatar": row.get("avatar", "👤"),
                    }
                    students.append(student)
                    class_names.add(row["class_name"])
            return students, sorted(class_names)
        
        return [], []


# 延迟加载学生数据，避免在模块导入时出错
ALL_STUDENTS = []
CLASS_LIST = []
_STUDENTS_INITIALIZED = False

def init_students_data():
    """初始化学生数据（延迟加载）"""
    global ALL_STUDENTS, CLASS_LIST, _STUDENTS_INITIALIZED
    if _STUDENTS_INITIALIZED:
        return
    try:
        ALL_STUDENTS, CLASS_LIST = load_students()
        _STUDENTS_INITIALIZED = True
        print(f"✅ 学生数据加载成功: {len(ALL_STUDENTS)} 个学生, {len(CLASS_LIST)} 个班级")
    except Exception as e:
        print(f"⚠️  学生数据加载失败，使用空列表: {e}")
        ALL_STUDENTS, CLASS_LIST = [], []
        import traceback
        traceback.print_exc()

def ensure_students_loaded():
    """确保学生数据已加载（在首次使用时调用）"""
    if not _STUDENTS_INITIALIZED:
        try:
            with app.app_context():
                init_students_data()
        except Exception as e:
            print(f"⚠️  延迟加载学生数据失败: {e}")

# 尝试在应用上下文中初始化学生数据
try:
    with app.app_context():
        init_students_data()
except Exception as e:
    print(f"⚠️  应用上下文初始化失败，将在首次使用时加载: {e}")
    # 如果应用上下文不可用，先使用空列表，稍后在首次请求时加载
    ALL_STUDENTS, CLASS_LIST = [], []


# 团队数据缓存
_teams_cache = None
_teams_cache_time = 0
_teams_cache_lock = Lock()
TEAMS_CACHE_TTL = 300  # 缓存有效期（秒）- 增加到300秒（5分钟）大幅减少数据库查询和CPU使用

def load_teams(use_cache=True):
    """从数据库加载团队数据（如果数据库不可用，则从JSON文件读取）
    
    Args:
        use_cache: 是否使用缓存（默认True，写操作时设为False）
    """
    global _teams_cache, _teams_cache_time
    
    # 检查缓存
    if use_cache and _teams_cache is not None:
        current_time = time.time()
        if current_time - _teams_cache_time < TEAMS_CACHE_TTL:
            return _teams_cache.copy()  # 返回副本，避免外部修改影响缓存
    
    # 如果数据库不可用，从JSON文件读取
    if not USE_DATABASE or not DB_AVAILABLE:
        try:
            with open(TEAMS_FILE, "r", encoding="utf-8") as f:
                teams = json.load(f)
                # 更新缓存
                with _teams_cache_lock:
                    _teams_cache = teams
                    _teams_cache_time = time.time()
                return teams
        except (FileNotFoundError, json.JSONDecodeError):
            return []
    
    # 使用数据库（优化：使用 eager loading 避免 N+1 查询）
    with app.app_context():
        try:
            if joinedload:
                # 使用 joinedload 一次性加载所有关联的成员数据
                teams_query = Team.query.options(joinedload(Team.members)).all()
            else:
                teams_query = Team.query.all()
            
            teams = [team.to_dict() for team in teams_query]
            
            # 更新缓存
            with _teams_cache_lock:
                _teams_cache = teams
                _teams_cache_time = time.time()
            
            return teams
        except Exception as e:
            print(f"⚠️  从数据库读取团队数据失败，回退到文件存储: {e}")
            # 回退到文件存储
            try:
                with open(TEAMS_FILE, "r", encoding="utf-8") as f:
                    teams = json.load(f)
                    # 更新缓存
                    with _teams_cache_lock:
                        _teams_cache = teams
                        _teams_cache_time = time.time()
                    return teams
            except (FileNotFoundError, json.JSONDecodeError):
                return []


def invalidate_teams_cache():
    """使团队数据缓存失效"""
    global _teams_cache, _teams_cache_time
    with _teams_cache_lock:
        _teams_cache = None
        _teams_cache_time = 0


def save_teams(teams):
    """保存团队数据到数据库（兼容旧格式的teams列表）"""
    # 使缓存失效
    invalidate_teams_cache()
    
    # 如果数据库不可用，保存到JSON文件
    if not USE_DATABASE or not DB_AVAILABLE:
        try:
            with open(TEAMS_FILE, "w", encoding="utf-8") as f:
                json.dump(teams, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"⚠️  保存团队数据到文件失败: {e}")
        return
    
    # 使用数据库
    with app.app_context():
        try:
            for team_data in teams:
                team_id = team_data.get("id")
                if not team_id:
                    continue
                
                # 查找或创建团队
                team = Team.query.filter_by(id=team_id).first()
                if not team:
                    # 解析创建时间
                    created_at_str = team_data.get("created_at", "")
                    try:
                        created_at = datetime.strptime(created_at_str, "%Y-%m-%d %H:%M:%S")
                    except:
                        created_at = datetime.now()
                    
                    team = Team(
                        id=team_id,
                        class_name=team_data.get("class_name"),
                        team_name=team_data.get("team_name"),
                        game_name=team_data.get("game_name", "待定游戏"),
                        game_desc=team_data.get("game_desc", ""),
                        ai_game_name=team_data.get("ai_game_name", ""),
                        ai_game_desc=team_data.get("ai_game_desc", ""),
                        ai_chat_notes=team_data.get("ai_chat_notes", ""),
                        access_code=team_data.get("access_code", ""),
                        submitter=team_data.get("submitter", ""),
                        created_at=created_at
                    )
                    db.session.add(team)
                else:
                    # 更新现有团队
                    team.class_name = team_data.get("class_name", team.class_name)
                    team.team_name = team_data.get("team_name", team.team_name)
                    team.game_name = team_data.get("game_name", team.game_name)
                    team.game_desc = team_data.get("game_desc", team.game_desc)
                    team.ai_game_name = team_data.get("ai_game_name", team.ai_game_name)
                    team.ai_game_desc = team_data.get("ai_game_desc", team.ai_game_desc)
                    team.ai_chat_notes = team_data.get("ai_chat_notes", team.ai_chat_notes)
                    team.access_code = team_data.get("access_code", team.access_code)
                    team.submitter = team_data.get("submitter", team.submitter)
                    team.updated_at = datetime.now()
                
                # 更新团队成员
                TeamMember.query.filter_by(team_id=team_id).delete()
                members = team_data.get("members", [])
                for member_data in members:
                    member = TeamMember(
                        team_id=team_id,
                        name=member_data.get("name", ""),
                        is_leader=member_data.get("is_leader", False)
                    )
                    db.session.add(member)
            
            db.session.commit()
        except Exception as e:
            print(f"⚠️  保存团队数据到数据库失败，回退到文件存储: {e}")
            # 回退到文件存储
            try:
                with open(TEAMS_FILE, "w", encoding="utf-8") as f:
                    json.dump(teams, f, ensure_ascii=False, indent=2)
            except Exception as e2:
                print(f"⚠️  保存团队数据到文件也失败: {e2}")


def save_conversations(team_id, conversations_data):
    """保存团队的对话数据到数据库"""
    with app.app_context():
        conversations = conversations_data.get("conversations", [])
        
        for conv_data in conversations:
            conversation_id = conv_data.get("conversation_id")
            if not conversation_id:
                continue
            
            # 检查是否已存在
            existing = Conversation.query.filter_by(
                team_id=team_id,
                conversation_id=str(conversation_id)
            ).first()
            
            if existing:
                # 更新现有对话
                existing.conversation_name = conv_data.get("conversation_name", existing.conversation_name)
                existing.session_name = conversations_data.get("session_name", existing.session_name)
                existing.saved_at = datetime.now()
                conversation = existing
            else:
                # 创建新对话
                created_at_str = conv_data.get("created_at", "")
                try:
                    created_at = datetime.fromisoformat(created_at_str.replace('Z', '+00:00'))
                except:
                    try:
                        created_at = datetime.strptime(created_at_str, "%Y-%m-%d %H:%M:%S")
                    except:
                        created_at = datetime.now()
                
                conversation = Conversation(
                    team_id=team_id,
                    conversation_id=str(conversation_id),
                    conversation_name=conv_data.get("conversation_name", "未命名会话"),
                    session_name=conversations_data.get("session_name", ""),
                    created_at=created_at,
                    saved_at=datetime.now()
                )
                db.session.add(conversation)
            
            db.session.flush()  # 获取conversation.id
            
            # 更新消息（删除旧消息，添加新消息）
            ConversationMessage.query.filter_by(conversation_id=conversation.id).delete()
            
            messages = conv_data.get("messages", [])
            for msg_data in messages:
                msg_created_at = None
                msg_created_at_str = msg_data.get("created_at")
                if msg_created_at_str:
                    try:
                        msg_created_at = datetime.fromisoformat(msg_created_at_str.replace('Z', '+00:00'))
                    except:
                        try:
                            msg_created_at = datetime.strptime(msg_created_at_str, "%Y-%m-%d %H:%M:%S")
                        except:
                            pass
                
                message = ConversationMessage(
                    conversation_id=conversation.id,
                    role=msg_data.get("role", "user"),
                    content=msg_data.get("content") or msg_data.get("text", ""),
                    message_id=msg_data.get("message_id", ""),
                    created_at=msg_created_at
                )
                db.session.add(message)
        
        db.session.commit()
    
    # 同时保存到JSON文件（作为备份）
    conversations_file = os.path.join(CONVERSATIONS_DIR, f"team_{team_id}_conversations.json")
    with open(conversations_file, "w", encoding="utf-8") as f:
        json.dump(conversations_data, f, ensure_ascii=False, indent=2)
    
    return conversations_file


def load_conversations(team_id):
    """从数据库加载团队的对话数据"""
    with app.app_context():
        conversations = Conversation.query.filter_by(team_id=team_id).all()
        
        if not conversations:
            # 如果数据库中没有，尝试从JSON文件加载（向后兼容）
            conversations_file = os.path.join(CONVERSATIONS_DIR, f"team_{team_id}_conversations.json")
            if os.path.exists(conversations_file):
                try:
                    with open(conversations_file, "r", encoding="utf-8") as f:
                        return json.load(f)
                except:
                    pass
            return None
        
        # 从数据库构建对话数据
        conversations_data = {
            "team_id": team_id,
            "conversations": [conv.to_dict() for conv in conversations],
            "total_conversations": len(conversations),
            "saved_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        }
        
        # 获取团队信息
        team = Team.query.filter_by(id=team_id).first()
        if team:
            conversations_data["team_name"] = team.team_name
            conversations_data["class_name"] = team.class_name
            conversations_data["access_code"] = team.access_code
            conversations_data["session_name"] = team.access_code
        
        return conversations_data


def generate_word_document(team_data, conversations_data):
    """生成Word文档"""
    doc = Document()
    
    # 设置文档标题
    title = doc.add_heading(f'{team_data.get("team_name", "未知团队")} - AI对话记录', 0)
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    
    # 添加团队信息
    doc.add_heading('团队信息', level=1)
    team_info = doc.add_paragraph()
    team_info.add_run('班级：').bold = True
    team_info.add_run(f'{team_data.get("class_name", "未知")}\n')
    team_info.add_run('团队名称：').bold = True
    team_info.add_run(f'{team_data.get("team_name", "未知")}\n')
    team_info.add_run('访问码：').bold = True
    team_info.add_run(f'{team_data.get("access_code", "未知")}\n')
    
    # 添加成员信息
    members = team_data.get("members", [])
    if members:
        team_info.add_run('团队成员：').bold = True
        member_names = []
        for member in members:
            name = member.get("name", "")
            if member.get("is_leader", False):
                name += "（队长）"
            member_names.append(name)
        team_info.add_run("、".join(member_names) + "\n")
    
    team_info.add_run('生成时间：').bold = True
    team_info.add_run(datetime.now().strftime("%Y年%m月%d日 %H:%M:%S"))
    
    # 添加对话记录
    conversations = conversations_data.get("conversations", [])
    if conversations:
        doc.add_heading('对话记录', level=1)
        
        for idx, conv in enumerate(conversations, 1):
            # 会话标题
            conv_title = doc.add_heading(f'会话 {idx}: {conv.get("conversation_name", "未命名会话")}', level=2)
            
            # 会话信息
            conv_info = doc.add_paragraph()
            conv_info.add_run('创建时间：').bold = True
            created_at = conv.get("created_at", "未知")
            conv_info.add_run(f'{created_at}\n')
            conv_info.add_run('消息数量：').bold = True
            conv_info.add_run(f'{conv.get("message_count", 0)}条\n')
            
            # 消息内容
            messages = conv.get("messages", [])
            if messages:
                doc.add_heading('消息内容', level=3)
                
                for msg in messages:
                    # 消息角色和内容
                    msg_para = doc.add_paragraph()
                    
                    # 根据角色设置不同的样式
                    role = msg.get("role", "user")
                    content = msg.get("content") or msg.get("text") or "（无内容）"
                    
                    if role == "user":
                        # 学生消息 - 蓝色
                        msg_para.add_run('👤 学生：').bold = True
                        msg_para.runs[0].font.color.rgb = RGBColor(0, 102, 204)
                        msg_content = msg_para.add_run(content)
                        msg_content.font.color.rgb = RGBColor(0, 102, 204)
                    else:
                        # AI消息 - 绿色
                        msg_para.add_run('🤖 AI助手：').bold = True
                        msg_para.runs[0].font.color.rgb = RGBColor(0, 153, 0)
                        msg_content = msg_para.add_run(content)
                        msg_content.font.color.rgb = RGBColor(0, 153, 0)
                    
                    # 添加时间戳（如果有）
                    if msg.get("created_at"):
                        time_para = doc.add_paragraph()
                        time_run = time_para.add_run(f'  时间：{msg.get("created_at")}')
                        time_run.font.size = Pt(9)
                        time_run.font.color.rgb = RGBColor(128, 128, 128)
                    
                    # 添加分隔线
                    doc.add_paragraph("─" * 50).runs[0].font.color.rgb = RGBColor(200, 200, 200)
            else:
                doc.add_paragraph('（该会话暂无消息）', style='Intense Quote')
            
            # 会话之间的分隔
            if idx < len(conversations):
                doc.add_page_break()
    else:
        doc.add_paragraph('暂无对话记录', style='Intense Quote')
    
    # 添加页脚
    doc.add_paragraph()
    footer = doc.add_paragraph('本文档由Python+AI小游戏设计课程系统自动生成')
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    footer.runs[0].font.size = Pt(9)
    footer.runs[0].font.color.rgb = RGBColor(128, 128, 128)
    
    return doc


def get_teamed_students(teams, class_name):
    """获取指定班级已组队学生名单"""
    teamed_students = set()
    for team in teams:
        if team.get("class_name") != class_name:
            continue
        for member in team.get("members", []):
            teamed_students.add(member["name"])
    return teamed_students


def require_login():
    """简单的登录校验"""
    if "student" not in session:
        return False
    return True


def generate_access_code(class_name, teams):
    """
    生成格式化的访问码，格式：team_班级编号_团队序号
    例如：team_509_01, team_509_02
    
    Args:
        class_name: 班级名称，如"五年级9班"
        teams: 所有团队列表
    
    Returns:
        格式化的访问码字符串
    """
    # 从班级名称提取编号（如"五年级9班" -> "509"）
    # 支持多种格式：五年级9班 -> 509, 一年级1班 -> 101
    class_num = ""
    if "年级" in class_name and "班" in class_name:
        # 提取年级和班级数字
        import re
        parts = re.findall(r'\d+', class_name)
        if len(parts) >= 2:
            # 如果有两个数字，第一个是年级，第二个是班级
            class_num = parts[0] + parts[1].zfill(2)  # 班级号补零到2位
        elif len(parts) == 1:
            # 如果只有一个数字，假设是班级号，年级默认为5
            class_num = "5" + parts[0].zfill(2)
    else:
        # 如果格式不标准，尝试提取所有数字
        nums = re.findall(r'\d+', class_name)
        if nums:
            class_num = nums[0] + (nums[1].zfill(2) if len(nums) > 1 else "01")
        else:
            class_num = "509"  # 默认值
    
    # 统计该班级已有团队数量
    class_teams = [t for t in teams if t.get("class_name") == class_name]
    team_count = len(class_teams)
    
    # 生成序号（从01开始）
    team_seq = str(team_count + 1).zfill(2)
    
    # 生成访问码：team_班级编号_团队序号
    access_code = f"team_{class_num}_{team_seq}"
    
    # 确保唯一性（虽然理论上应该唯一，但还是要检查）
    existing_codes = {t.get("access_code") for t in teams if t.get("access_code")}
    if access_code in existing_codes:
        # 如果冲突，递增序号
        seq_num = team_count + 1
        while f"team_{class_num}_{str(seq_num).zfill(2)}" in existing_codes:
            seq_num += 1
        access_code = f"team_{class_num}_{str(seq_num).zfill(2)}"
    
    return access_code


def generate_coze_jwt_token(team_id, class_name, team_name, access_code):
    """
    生成Coze OAuth JWT Token，用于团队级会话隔离
    
    Args:
        team_id: 团队ID
        class_name: 班级名称
        team_name: 团队名称
        access_code: 团队访问码
    
    Returns:
        JWT Token字符串，如果OAuth未配置则返回None
    """
    # 检查OAuth配置（APP_ID和KID必须有，私钥可以从文件读取）
    if not COZE_OAUTH_APP_ID or not COZE_OAUTH_KID:
        # 如果没有配置OAuth，返回None，使用原来的token方式
        return None
    
    try:
        # session_name 直接使用访问码（每个团队有唯一的访问码）
        session_name = str(access_code)
        
        # JWT Payload
        now = int(time.time())
        payload = {
            "iss": COZE_OAUTH_APP_ID,  # OAuth应用ID
            "aud": "api.coze.cn",  # 固定值
            "iat": now,  # 生效时间（不能超过当前时间）
            "exp": now + 3600,  # 过期时间（建议≤3600秒，需晚于iat）
            "jti": str(uuid.uuid4()),  # 防重放攻击，32位以上随机值
            "session_name": session_name,  # 团队唯一标识（核心字段）
            "session_context": {  # 可选：附加团队元数据
                "team_id": str(team_id),
                "team_name": team_name,
                "class_name": class_name,
                "access_code": access_code
            }
        }
        
        # 读取RSA私钥
        # 优先从文件读取（更可靠），如果文件不存在则从环境变量读取
        private_key = None
        private_key_file = os.path.join(os.path.dirname(__file__), 'private_key.pem')
        
        # 首先尝试从文件读取
        if os.path.exists(private_key_file):
            try:
                with open(private_key_file, 'r', encoding='utf-8') as f:
                    private_key = f.read().strip()
                print(f"✅ 已从文件读取私钥: {private_key_file}")
            except Exception as e:
                print(f"⚠️ 从文件读取私钥失败: {e}")
        
        # 如果文件读取失败，尝试从环境变量读取
        if not private_key:
            private_key = COZE_OAUTH_PRIVATE_KEY
            if private_key:
                # 如果私钥是环境变量中的字符串，需要处理换行符（\\n -> \n）
                private_key = private_key.replace('\\n', '\n').strip()
                print(f"✅ 已从环境变量读取私钥")
        
        # 验证私钥格式
        if not private_key:
            error_msg = "❌ 私钥未找到：请确保 private_key.pem 文件存在或设置 COZE_OAUTH_PRIVATE_KEY 环境变量"
            print(error_msg)
            raise ValueError(error_msg)
        
        if not private_key.strip().startswith('-----BEGIN'):
            error_msg = "❌ 私钥格式不正确：必须包含 -----BEGIN PRIVATE KEY----- 和 -----END PRIVATE KEY-----"
            print(error_msg)
            print(f"   私钥开头: {private_key[:50]}...")
            raise ValueError(error_msg)
        
        # 生成JWT Token（使用RS256算法）
        # 参考 generate_jwt.py 的格式，headers必须包含alg、typ和kid
        headers = {
            "alg": "RS256",  # 固定加密算法，必须选这个
            "typ": "JWT",    # 固定类型
            "kid": COZE_OAUTH_KID if COZE_OAUTH_KID else ""  # 公钥指纹
        }
        
        token = jwt.encode(
            payload,
            private_key,
            algorithm="RS256",
            headers=headers
        )
        
        # 调试：打印JWT信息（不打印完整token）
        print(f"✅ JWT Token生成成功")
        print(f"   session_name: {session_name}")
        print(f"   team_id: {team_id}")
        print(f"   team_name: {team_name}")
        print(f"   class_name: {class_name}")
        print(f"   access_code: {access_code}")
        print(f"   Token前20字符: {token[:20]}...")
        print(f"   Token长度: {len(token)}")
        
        return token
        
    except Exception as e:
        print(f"生成JWT Token失败: {e}")
        import traceback
        traceback.print_exc()
        return None


def get_coze_access_token(jwt_token):
    """
    使用JWT Token调用Coze Token API获取access_token
    
    Args:
        jwt_token: 生成的JWT Token字符串
    
    Returns:
        access_token字符串，如果失败则返回None
    """
    if not jwt_token:
        return None
    
    try:
        # 调用Coze Token API
        url = "https://api.coze.cn/api/permission/oauth2/token"
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {jwt_token}"
        }
        data = {
            "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
            "duration_seconds": 3600  # Token有效期（≤86400秒，建议3600秒）
        }
        
        print(f"\n🔍 调用Coze Token API获取access_token...")
        print(f"   URL: {url}")
        print(f"   grant_type: {data['grant_type']}")
        print(f"   duration_seconds: {data['duration_seconds']}")
        
        response = requests.post(url, headers=headers, json=data, timeout=10)
        
        print(f"   API响应状态码: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            if "access_token" in result:
                access_token = result["access_token"]
                print(f"✅ 成功获取access_token")
                print(f"   access_token前30字符: {access_token[:30]}...")
                print(f"   access_token长度: {len(access_token)}")
                # 检查是否有其他信息
                if "expires_in" in result:
                    print(f"   access_token有效期: {result['expires_in']}秒")
                if "token_type" in result:
                    print(f"   token_type: {result['token_type']}")
                return access_token
            else:
                print(f"❌ API响应中未找到access_token")
                print(f"   响应内容: {result}")
                return None
        else:
            print(f"❌ Token API调用失败")
            print(f"   状态码: {response.status_code}")
            print(f"   响应内容: {response.text}")
            try:
                error_detail = response.json()
                print(f"   错误详情: {error_detail}")
                # 根据错误码提供解决建议
                if response.status_code == 401:
                    print(f"   ⚠️ 401错误：JWT Token无效或签名错误")
                    print(f"   请检查：")
                    print(f"   1. JWT Header中的kid是否与OAuth应用配置的公钥指纹一致")
                    print(f"   2. JWT Payload中的iss是否为OAuth应用ID（不是智能体ID）")
                    print(f"   3. 私钥是否正确（检查private_key.pem文件）")
                elif response.status_code == 403:
                    print(f"   ⚠️ 403错误：OAuth应用权限不足或被禁用")
                    print(f"   请检查：")
                    print(f"   1. OAuth应用是否已启用（在扣子平台 → API管理 → OAuth应用）")
                    print(f"   2. OAuth应用是否已授权（需要bot:chat、conversation:create等权限）")
                    print(f"   3. 公钥是否已正确上传到OAuth应用配置")
            except:
                pass
            return None
            
    except requests.exceptions.RequestException as e:
        print(f"❌ 调用Token API时发生网络错误: {e}")
        import traceback
        traceback.print_exc()
        return None
    except Exception as e:
        print(f"❌ 获取access_token失败: {e}")
        import traceback
        traceback.print_exc()
        return None


def get_class_students(class_name):
    """按班级获取学生列表"""
    ensure_students_loaded()
    return [s for s in ALL_STUDENTS if s["class_name"] == class_name]


@app.route("/")
def index():
    """主页面 - 直接显示第二节课页面"""
    ensure_students_loaded()
    return render_template("second2.html")


@app.route("/team")
def team():
    """组队页面"""
    if not require_login():
        return redirect(url_for("login"))
    return render_template("team.html")


@app.route("/ai-chat")
def ai_chat():
    """AI 对话页面 - 自动使用团队访问码作为session_name"""
    try:
        # 允许通过访问码或team_id访问（用于从详情页跳转）
        team_id = request.args.get("team_id", type=int)
        access_code = request.args.get("access_code", "").strip()
        
        # 如果提供了访问码，先查询team_id
        if access_code and not team_id:
            try:
                if len(access_code) == 5:
                    class_num = access_code[:3]
                    team_seq = access_code[3:]
                    db_access_code = f"team_{class_num}_{team_seq}"
                else:
                    db_access_code = access_code
                
                teams = load_teams()
                team = next((t for t in teams if t.get("access_code") == db_access_code), None)
                if team:
                    team_id = team.get("id")
            except Exception as e:
                print(f"通过访问码查询team_id失败: {e}")
        
        if not team_id:
            print(f"❌ AI对话页面：缺少team_id参数")
            return redirect(url_for("index"))  # 重定向到首页（second2.html）
        
        # 只加载一次团队数据
        teams = load_teams()
        print(f"✅ 加载了 {len(teams)} 个团队")
        
        # 查找团队（如果有session，验证班级；否则只验证team_id）
        if "student" in session:
            class_name = session["student"]["class_name"]
            print(f"✅ 当前学生班级: {class_name}, 查找团队ID: {team_id}")
            target = next((t for t in teams if t.get("id") == team_id and t.get("class_name") == class_name), None)
        else:
            print(f"✅ 未登录用户，查找团队ID: {team_id}")
            target = next((t for t in teams if t.get("id") == team_id), None)
        
        if not target:
            print(f"❌ AI对话页面：未找到团队 (team_id={team_id})")
            print(f"   可用团队: {[t.get('id') for t in teams]}")
            return redirect(url_for("index"))  # 重定向到首页（second2.html）
        
        # 确保团队有访问码（用于JWT session_name），如果没有则自动生成
        team_access_code = target.get("access_code")
        if not team_access_code:
            # 不需要重新加载，直接使用已有的 teams 列表
            new_access_code = generate_access_code(class_name, teams)
            # 更新团队数据（直接更新数据库，不使用文件）
            if USE_DATABASE and DB_AVAILABLE:
                try:
                    with app.app_context():
                        team_obj = Team.query.filter_by(id=team_id, class_name=class_name).first()
                        if team_obj:
                            team_obj.access_code = new_access_code
                            db.session.commit()
                            # 使缓存失效
                            invalidate_teams_cache()
                            target["access_code"] = new_access_code
                            team_access_code = new_access_code
                except Exception as e:
                    print(f"⚠️  更新访问码失败: {e}")
                    # 回退到文件方式
                    for t in teams:
                        if t.get("id") == team_id and t.get("class_name") == class_name:
                            t["access_code"] = new_access_code
                            save_teams(teams)
                            target["access_code"] = new_access_code
                            team_access_code = new_access_code
                            break
            else:
                # 文件方式
                for t in teams:
                    if t.get("id") == team_id and t.get("class_name") == class_name:
                        t["access_code"] = new_access_code
                        save_teams(teams)
                        target["access_code"] = new_access_code
                        team_access_code = new_access_code
                        break
        
        # 直接进入AI对话界面（不再需要访问码验证）
        # 生成团队级别的JWT Token（用于会话隔离）
        print(f"\n{'='*60}")
        print(f"🔍 进入AI对话页面")
        print(f"   团队ID: {target.get('id')}")
        print(f"   团队名称: {target.get('team_name')}")
        print(f"   班级: {target.get('class_name')}")
        print(f"   访问码: {target.get('access_code')}")
        print(f"{'='*60}")
        
        # 生成JWT Token（用于团队级会话隔离）
        jwt_token = generate_coze_jwt_token(
            team_id=target.get("id"),
            class_name=target.get("class_name"),
            team_name=target.get("team_name"),
            access_code=target.get("access_code", "")
        )
        
        # 使用JWT Token获取access_token
        access_token = None
        use_jwt = False
        
        if jwt_token:
            print(f"✅ JWT Token生成成功，正在获取access_token...")
            # 调用Token API获取access_token
            access_token = get_coze_access_token(jwt_token)
            if access_token:
                use_jwt = True
                auth_token = access_token  # 使用access_token
                print(f"✅ 使用JWT OAuth流程，access_token已获取")
                print(f"   ⚠️ 会话隔离：通过JWT的session_name实现（访问码: {target.get('access_code')}）")
            else:
                print(f"❌ 获取access_token失败！")
                print(f"   ⚠️ 回退到普通Token（无法实现会话隔离）")
                auth_token = COZE_WEB_TOKEN  # 回退到普通Token
        else:
            print(f"❌ JWT Token生成失败！")
            print(f"   ⚠️ 使用普通Token（无法实现会话隔离）")
            print(f"   可能原因：")
            print(f"   1. OAuth环境变量未设置（COZE_OAUTH_APP_ID, COZE_OAUTH_KID）")
            print(f"   2. private_key.pem文件不存在或格式错误")
            auth_token = COZE_WEB_TOKEN  # 使用普通Token
        
        print(f"   最终使用的Token类型: {'JWT OAuth (access_token)' if use_jwt else '普通Token (cztei_...)'}")
        print(f"   Token前30字符: {auth_token[:30]}...")
        print(f"   Token长度: {len(auth_token)}")
        if use_jwt:
            print(f"   会话隔离通过JWT session_name实现（访问码: {target.get('access_code')}）")
        else:
            print(f"   会话隔离通过userInfo.id实现（访问码: {target.get('access_code')}）")
        print(f"{'='*60}\n")
        
        # 处理student信息（如果未登录，使用队长身份）
        student_info = session.get("student")
        if not student_info:
            # 从团队成员中获取队长作为用户身份
            members = target.get("members", [])
            leader = next((m for m in members if isinstance(m, dict) and (m.get("is_leader") or m.get("isLeader"))), None)
            
            if not leader and members:
                # 如果没有找到队长，使用第一个成员
                leader = members[0] if isinstance(members[0], dict) else None
            
            # 使用队长身份设置session（这样保存时会使用队长身份）
            if leader:
                # 查找队长在学生列表中的信息
                leader_name = leader.get("name") if isinstance(leader, dict) else str(leader)
                ensure_students_loaded()
                leader_student = next((s for s in ALL_STUDENTS if s.get("name") == leader_name and s.get("class_name") == target.get("class_name")), None)
                
                if leader_student:
                    # 使用队长的完整学生信息
                    student_info = {
                        "name": leader_student.get("name"),
                        "class_name": leader_student.get("class_name"),
                        "student_id": leader_student.get("student_id"),
                        "avatar": leader_student.get("avatar", "👤")
                    }
                    # 设置session，这样后续操作都使用队长身份
                    session["student"] = student_info
                    print(f"✅ 使用队长身份登录: {leader_name} (学号: {student_info.get('student_id')})")
                else:
                    # 如果找不到学生信息，创建临时对象
                    student_info = {
                        "name": leader_name,
                        "class_name": target.get("class_name", ""),
                        "student_id": 0,
                        "avatar": "👤"
                    }
                    session["student"] = student_info
                    print(f"⚠️  队长不在学生列表中，使用临时身份: {leader_name}")
            else:
                # 如果没有成员，使用团队名称
                student_info = {
                    "name": target.get("team_name", "用户"),
                    "class_name": target.get("class_name", ""),
                    "student_id": 0,
                    "avatar": "👤"
                }
                session["student"] = student_info
                print(f"⚠️  团队没有成员，使用团队名称: {target.get('team_name')}")
        
        return render_template(
            "ai_chat.html",
            team=target,
            student=student_info,
            coze_token=auth_token,  # 使用access_token（如果JWT成功）或普通Token（如果失败）
            coze_app_id=COZE_APP_ID,
            use_jwt=use_jwt,  # 标记是否使用JWT OAuth
            jwt_token=None,  # 不传递JWT Token给前端（前端只需要access_token）
        )
    except KeyError as e:
        print(f"❌ AI对话页面错误：KeyError - {e}")
        print(f"   Session内容: {list(session.keys())}")
        import traceback
        traceback.print_exc()
        return f"服务器错误：缺少必要信息 ({str(e)})", 500
    except Exception as e:
        print(f"❌ AI对话页面错误：{type(e).__name__} - {e}")
        import traceback
        traceback.print_exc()
        return f"服务器错误：{str(e)}", 500


@app.route("/login", methods=["GET"])
def login():
    """登录页面"""
    # 不记住上次登录，进入登录页即清除会话
    session.clear()
    ensure_students_loaded()
    return render_template("login.html", class_list=CLASS_LIST)


@app.route("/logout", methods=["POST"])
def logout():
    session.clear()
    return jsonify({"success": True})


@app.route("/api/login-check", methods=["POST"])
def login_check():
    """检查学号与班级，返回学生信息供前端确认"""
    data = request.get_json() or {}
    class_name = data.get("class_name", "")
    student_id = data.get("student_id")

    if not class_name or student_id is None:
        return jsonify({"error": "请提供班级和学号"}), 400

    try:
        student_id = int(student_id)
    except ValueError:
        return jsonify({"error": "学号必须是数字"}), 400

    ensure_students_loaded()
    for stu in ALL_STUDENTS:
        if stu["class_name"] == class_name and stu["student_id"] == student_id:
            return jsonify({"found": True, "student": stu})

    return jsonify({"found": False, "error": "未找到对应学生，请检查班级或学号"}), 404


@app.route("/api/login", methods=["POST"])
def do_login():
    """确认登录并写入 session"""
    data = request.get_json() or {}
    class_name = data.get("class_name", "")
    student_id = data.get("student_id")

    if not class_name or student_id is None:
        return jsonify({"error": "请提供班级和学号"}), 400

    try:
        student_id = int(student_id)
    except ValueError:
        return jsonify({"error": "学号必须是数字"}), 400

    ensure_students_loaded()
    for stu in ALL_STUDENTS:
        if stu["class_name"] == class_name and stu["student_id"] == student_id:
            session["student"] = {
                "class_name": class_name,
                "student_id": student_id,
                "name": stu["name"],
                "avatar": stu.get("avatar", "👤"),
            }
            return jsonify({"success": True, "student": session["student"]})

    return jsonify({"error": "未找到对应学生，请检查班级或学号"}), 404


@app.route("/api/me", methods=["GET"])
def me():
    """返回当前登录学生信息"""
    if not require_login():
        return jsonify({"logged_in": False}), 401
    return jsonify({"logged_in": True, "student": session["student"]})


@app.route("/api/students", methods=["GET"])
def get_students():
    """获取当前班级学生列表"""
    if not require_login():
        return jsonify({"error": "未登录"}), 401

    class_name = session["student"]["class_name"]
    class_students = get_class_students(class_name)

    teams = load_teams()
    teamed_students = get_teamed_students(teams, class_name)
    available_students = [s for s in class_students if s["name"] not in teamed_students]

    return jsonify(
        {
            "class_name": class_name,
            "all_students": class_students,
            "available_students": available_students,
            "teamed_students": list(teamed_students),
            "total_count": len(class_students),
            "available_count": len(available_students),
        }
    )


@app.route("/api/teams", methods=["GET"])
def get_teams():
    """获取当前班级团队列表"""
    if not require_login():
        return jsonify({"error": "未登录"}), 401

    class_name = session["student"]["class_name"]
    teams = load_teams()
    class_teams = [t for t in teams if t.get("class_name") == class_name]

    return jsonify({"teams": class_teams, "count": len(class_teams)})


@app.route("/api/teams", methods=["POST"])
def create_team():
    """创建新团队（仅当前班级）"""
    if not require_login():
        return jsonify({"error": "未登录"}), 401

    try:
        data = request.get_json() or {}

        team_name = data.get("team_name", "").strip()
        # 为适配当前只收团队名的页面，若未提供游戏信息则给默认占位
        game_name = data.get("game_name", "").strip() or "待定游戏"
        game_desc = data.get("game_desc", "").strip() or "请在后续课程中补充游戏描述"
        members = data.get("members", [])
        leader = data.get("leader", "")
        class_name = session["student"]["class_name"]

        if not team_name:
            return jsonify({"error": "团队名称不能为空"}), 400

        if not members or len(members) < 3 or len(members) > 5:
            return jsonify({"error": "团队成员数量必须在3-5人之间"}), 400

        if leader not in members:
            return jsonify({"error": "队长必须是团队成员之一"}), 400

        teams = load_teams()
        teamed_students = get_teamed_students(teams, class_name)

        for member in members:
            if member in teamed_students:
                return jsonify({"error": f"学生 {member} 已经加入其他团队"}), 400

        # 生成格式化的团队访问码（格式：team_班级编号_团队序号）
        teams = load_teams()
        access_code = generate_access_code(class_name, teams)
        
        # 创建团队对象（直接保存到数据库）
        team_id = int(datetime.now().timestamp() * 1000)
        with app.app_context():
            team = Team(
                id=team_id,
                class_name=class_name,
                team_name=team_name,
                game_name=game_name,
                game_desc=game_desc,
                access_code=access_code,
                submitter=session["student"]["name"],
                created_at=datetime.now()
            )
            db.session.add(team)
            
            # 添加团队成员
            for member_name in members:
                member = TeamMember(
                    team_id=team_id,
                    name=member_name,
                    is_leader=member_name == leader
                )
                db.session.add(member)
            
            db.session.commit()
            # 使缓存失效
            invalidate_teams_cache()
        
        # 返回团队数据（兼容格式）
        team_dict = {
            "id": team_id,
            "class_name": class_name,
            "team_name": team_name,
            "game_name": game_name,
            "game_desc": game_desc,
            "members": [
                {"name": member, "is_leader": member == leader} for member in members
            ],
            "created_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "submitter": session["student"]["name"],
            "access_code": access_code,
        }

        return jsonify({"success": True, "message": "组队成功！", "team": team_dict}), 201

    except Exception as e:
        return jsonify({"error": f"服务器错误: {str(e)}"}), 500


@app.route("/api/teams/<int:team_id>", methods=["DELETE"])
def delete_team(team_id):
    """删除团队（可选功能，用于管理）"""
    if not require_login():
        return jsonify({"error": "未登录"}), 401

    class_name = session["student"]["class_name"]
    
    with app.app_context():
        team = Team.query.filter_by(id=team_id, class_name=class_name).first()
        if team:
            db.session.delete(team)  # 级联删除团队成员
            db.session.commit()
            # 使缓存失效
            invalidate_teams_cache()
            return jsonify({"success": True, "message": "团队已删除"})
        else:
            return jsonify({"error": "团队不存在"}), 404


@app.route("/api/teams/<int:team_id>", methods=["GET"])
def get_team(team_id):
    """获取单个团队详情"""
    if not require_login():
        return jsonify({"error": "未登录"}), 401
    class_name = session["student"]["class_name"]
    teams = load_teams()
    target = next((t for t in teams if t.get("id") == team_id and t.get("class_name") == class_name), None)
    if not target:
        return jsonify({"error": "未找到团队"}), 404
    return jsonify({"team": target})


@app.route("/api/teams/<int:team_id>/ai-note", methods=["POST"])
def update_team_ai_note(team_id):
    """记录AI对话产出的游戏信息，并同步到游戏名称和游戏介绍"""
    # 允许通过访问码访问（不强制登录）
    class_name = None
    if "student" in session:
        class_name = session["student"]["class_name"]
    else:
        # 如果没有登录，通过team_id获取班级信息
        with app.app_context():
            team_obj = Team.query.filter_by(id=team_id).first()
            if team_obj:
                class_name = team_obj.class_name
            else:
                return jsonify({"error": "未找到团队"}), 404

    data = request.get_json() or {}
    ai_game_name = (data.get("ai_game_name") or "").strip()
    ai_game_desc = (data.get("ai_game_desc") or "").strip()
    ai_chat_notes = (data.get("ai_chat_notes") or "").strip()

    with app.app_context():
        team = Team.query.filter_by(id=team_id).first()
        if not team:
            return jsonify({"error": "未找到团队"}), 404
        
        # 保存AI对话相关的信息
        team.ai_game_name = ai_game_name
        team.ai_game_desc = ai_game_desc
        team.ai_chat_notes = ai_chat_notes
        
        # ✅ 关键：同步到游戏名称和游戏介绍（主字段）
        # 如果AI对话产出的游戏名称和介绍不为空，则同步到主字段
        if ai_game_name:
            team.game_name = ai_game_name
        if ai_game_desc:
            team.game_desc = ai_game_desc
        
        team.updated_at = datetime.now()
        db.session.commit()
        # 使缓存失效
        invalidate_teams_cache()
        
        print(f"✅ 保存游戏信息成功")
        print(f"   游戏名称: {team.game_name}")
        print(f"   游戏介绍: {team.game_desc[:50] if team.game_desc else ''}...")
        print(f"   AI游戏名称: {team.ai_game_name}")
        print(f"   AI游戏介绍: {team.ai_game_desc[:50] if team.ai_game_desc else ''}...")
    
    return jsonify({"success": True, "message": "保存成功，已同步到游戏名称和游戏介绍"})


@app.route("/api/teams/<int:team_id>/update", methods=["POST"])
def update_team(team_id):
    """更新团队信息（保存修改记录，不覆盖）"""
    data = request.get_json() or {}
    
    team_name = (data.get("team_name") or "").strip()
    game_name = (data.get("game_name") or "").strip()
    game_desc = (data.get("game_desc") or "").strip()
    members = data.get("members", [])
    
    if not team_name:
        return jsonify({"error": "团队名称不能为空"}), 400
    
    if not members or len(members) == 0:
        return jsonify({"error": "至少需要一个成员"}), 400
    
    try:
        with app.app_context():
            team = Team.query.filter_by(id=team_id).first()
            if not team:
                return jsonify({"error": "未找到团队"}), 404
            
            # 保存修改记录（不是覆盖，而是更新）
            # 更新团队名称
            if team_name:
                team.team_name = team_name
            
            # 更新游戏信息（如果用户修改了，更新原始字段，保留AI生成的字段）
            if game_name:
                # 如果原来有AI生成的游戏名称，保留；否则更新原始字段
                if not team.ai_game_name:
                    team.game_name = game_name
                else:
                    # 如果用户修改了，更新原始字段作为用户修改版本
                    team.game_name = game_name
            
            if game_desc:
                if not team.ai_game_desc:
                    team.game_desc = game_desc
                else:
                    team.game_desc = game_desc
            
            # 更新团队成员（删除旧成员，添加新成员）
            TeamMember.query.filter_by(team_id=team_id).delete()
            for member_data in members:
                member = TeamMember(
                    team_id=team_id,
                    name=member_data.get("name", ""),
                    is_leader=member_data.get("is_leader", False)
                )
                db.session.add(member)
            
            team.updated_at = datetime.now()
            db.session.commit()
            
            # 使缓存失效
            invalidate_teams_cache()
        
        return jsonify({"success": True, "message": "团队信息已更新"})
    except Exception as e:
        print(f"更新团队失败: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({"error": f"更新失败: {str(e)}"}), 500


# -------- 教师管理接口（简易，无鉴权） --------
@app.route("/teacher")
def teacher():
    ensure_students_loaded()
    return render_template("teacher.html", classes=CLASS_LIST)


@app.route("/teacher/lesson2")
def teacher_lesson2():
    """教师管理面板 - 第2课"""
    ensure_students_loaded()
    return render_template("teacher_lesson2.html", classes=CLASS_LIST)


@app.route("/second")
def second():
    """第二节课页面"""
    ensure_students_loaded()
    return render_template("second2.html")


@app.route("/game-rules")
def game_rules():
    """游戏规则与变量页面"""
    ensure_students_loaded()
    return render_template("game-rules.html")


@app.route("/game-rules-detail")
def game_rules_detail():
    """游戏规则与变量详情页面"""
    access_code = request.args.get("access_code")
    if not access_code:
        return redirect(url_for("second"))
    
    # 验证访问码并获取团队信息
    try:
        # 将访问码转换为数据库格式
        if len(access_code) == 5:
            class_num = access_code[:3]
            team_seq = access_code[3:]
            db_access_code = f"team_{class_num}_{team_seq}"
        else:
            return redirect(url_for("second"))
        
        # 查询团队
        teams = load_teams()
        team = next((t for t in teams if t.get("access_code") == db_access_code), None)
        
        if not team:
            return redirect(url_for("second"))
        
        # ✅ 关键：使用队长身份设置session
        # 这样后续的AI对话和保存操作都会使用队长身份
        if "student" not in session:
            members = team.get("members", [])
            leader = next((m for m in members if isinstance(m, dict) and (m.get("is_leader") or m.get("isLeader"))), None)
            
            if leader:
                leader_name = leader.get("name") if isinstance(leader, dict) else str(leader)
                # 查找队长在学生列表中的信息
                ensure_students_loaded()
                leader_student = next((s for s in ALL_STUDENTS if s.get("name") == leader_name and s.get("class_name") == team.get("class_name")), None)
                
                if leader_student:
                    # 使用队长的完整学生信息设置session
                    session["student"] = {
                        "name": leader_student.get("name"),
                        "class_name": leader_student.get("class_name"),
                        "student_id": leader_student.get("student_id"),
                        "avatar": leader_student.get("avatar", "👤")
                    }
                    print(f"✅ 访问码登录：使用队长身份 - {leader_name} (学号: {leader_student.get('student_id')})")
                else:
                    # 如果找不到学生信息，创建临时session
                    session["student"] = {
                        "name": leader_name,
                        "class_name": team.get("class_name", ""),
                        "student_id": 0,
                        "avatar": "👤"
                    }
                    print(f"⚠️  队长不在学生列表中，使用临时身份: {leader_name}")
        
        return render_template("game-rules-detail.html", team=team, access_code=access_code)
    except Exception as e:
        print(f"加载团队详情失败: {e}")
        return redirect(url_for("second"))


@app.route("/further-learning")
def further_learning():
    """进一步学习页面"""
    access_code = request.args.get("access_code", "")
    return render_template("further-learning.html", access_code=access_code)

@app.route("/ai-study")
def ai_study():
    """AI学习助手页面 - 使用iframe嵌入Dify智能体，实现团队级会话隔离"""
    team_id = request.args.get("team_id", type=int)
    access_code = request.args.get("access_code", "").strip()
    team_info = None
    
    # 如果提供了访问码，先查询team_id和团队信息
    if access_code and not team_id:
        try:
            if len(access_code) == 5:
                class_num = access_code[:3]
                team_seq = access_code[3:]
                db_access_code = f"team_{class_num}_{team_seq}"
            else:
                db_access_code = access_code
            
            teams = load_teams()
            team = next((t for t in teams if t.get("access_code") == db_access_code), None)
            if team:
                team_id = team.get("id")
                team_info = team
        except Exception as e:
            print(f"通过访问码查询team_id失败: {e}")
    
    # 如果只有team_id，获取团队信息
    if team_id and not team_info:
        teams = load_teams()
        team_info = next((t for t in teams if t.get("id") == team_id), None)
        if team_info and not access_code:
            access_code = team_info.get("access_code", "")
    
    # 获取团队的访问码（用于会话隔离）
    team_access_code = access_code
    if team_info and not team_access_code:
        team_access_code = team_info.get("access_code", "")
    
    # ✅ 关键修复：如果没有访问码，自动生成一个唯一的访问码并保存
    if not team_access_code and team_id and team_info:
        print(f"⚠️  团队 {team_id} 没有访问码，正在生成...")
        teams = load_teams()
        class_name = team_info.get("class_name", "")
        new_access_code = generate_access_code(class_name, teams)
        
        # 保存到数据库
        try:
            with app.app_context():
                team_obj = Team.query.filter_by(id=team_id).first()
                if team_obj:
                    team_obj.access_code = new_access_code
                    db.session.commit()
                    invalidate_teams_cache()
                    team_access_code = new_access_code
                    team_info["access_code"] = new_access_code
                    print(f"✅ 已为团队 {team_id} 生成并保存访问码: {new_access_code}")
                else:
                    # 如果数据库中没有，使用临时标识
                    team_access_code = f"team_{team_id}"
                    print(f"⚠️  数据库中没有找到团队 {team_id}，使用临时标识: {team_access_code}")
        except Exception as e:
            print(f"❌ 生成访问码失败: {e}")
            team_access_code = f"team_{team_id}"
    elif not team_access_code and team_id:
        # 如果连team_info都没有，使用临时标识
        team_access_code = f"team_{team_id}"
        print(f"⚠️  使用临时访问码: {team_access_code}")
    
    # Dify智能体基础URL（可从环境变量配置）
    # 尝试从环境变量读取，如果不存在则尝试从.env文件读取
    base_url = os.environ.get("DIFY_CHATBOT_URL")
    if not base_url:
        # 如果环境变量不存在，尝试从.env文件直接读取
        env_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), '.env')
        if os.path.exists(env_file):
            try:
                with open(env_file, 'r', encoding='utf-8') as f:
                    for line in f:
                        line = line.strip()
                        if line and not line.startswith('#') and 'DIFY_CHATBOT_URL=' in line:
                            base_url = line.split('=', 1)[1].strip().strip('"').strip("'")
                            print(f"✅ 从.env文件读取DIFY_CHATBOT_URL: {base_url}")
                            break
            except Exception as e:
                print(f"⚠️  读取.env文件失败: {e}")
    
    # 如果还是没有，使用默认值
    if not base_url:
        base_url = "http://localhost/chatbot/DNGyJpSDOa9BsC0H"
        print(f"⚠️  使用默认Dify URL: {base_url}")
    
    print(f"🔍 当前使用的Dify URL: {base_url}")
    
    # Dify通过user参数实现会话隔离
    # 格式：http://localhost/chatbot/DNGyJpSDOa9BsC0H?user=team_511_01
    if team_access_code:
        # 如果URL中已有参数，使用&连接，否则使用?连接
        separator = "&" if "?" in base_url else "?"
        dify_chatbot_url = f"{base_url}{separator}user={team_access_code}"
    else:
        dify_chatbot_url = base_url
    
    print(f"\n{'='*60}")
    print(f"🔍 AI学习助手页面（Dify）")
    print(f"   团队ID: {team_id}")
    print(f"   访问码: {team_access_code}")
    print(f"   Dify iframe URL: {dify_chatbot_url}")
    print(f"   ✅ 会话隔离：通过user={team_access_code}实现")
    print(f"{'='*60}\n")
    
    return render_template("ai_study.html", 
                         team_id=team_id, 
                         access_code=access_code,
                         team_access_code=team_access_code,
                         dify_chatbot_url=dify_chatbot_url)

@app.route("/api/teams/submit-variables-rules", methods=["POST"])
def submit_variables_rules():
    """提交变量和规则"""
    data = request.get_json() or {}
    
    print(f"\n{'='*60}")
    print(f"收到提交变量和规则请求")
    print(f"请求数据: {data}")
    print(f"{'='*60}")
    
    # 正确获取team_id（可能是字符串或数字）
    team_id_raw = data.get("team_id")
    team_id = None
    if team_id_raw:
        try:
            team_id = int(team_id_raw)
        except (ValueError, TypeError):
            print(f"⚠️  team_id转换失败: {team_id_raw}")
            team_id = None
    
    access_code = data.get("access_code", "").strip()
    variables = data.get("variables", "").strip()
    rules = data.get("rules", "").strip()
    
    print(f"team_id: {team_id}")
    print(f"access_code: {access_code}")
    print(f"variables长度: {len(variables)}")
    print(f"rules长度: {len(rules)}")
    
    if not variables or not rules:
        print(f"❌ 变量或规则为空")
        return jsonify({"error": "变量和规则内容不能为空"}), 400
    
    # 通过team_id或access_code查找团队
    try:
        with app.app_context():
            team = None
            if team_id:
                print(f"通过team_id查找: {team_id}")
                team = Team.query.filter_by(id=team_id).first()
            elif access_code:
                print(f"通过access_code查找: {access_code}")
                if len(access_code) == 5:
                    class_num = access_code[:3]
                    team_seq = access_code[3:]
                    db_access_code = f"team_{class_num}_{team_seq}"
                else:
                    db_access_code = access_code
                print(f"转换后的access_code: {db_access_code}")
                team = Team.query.filter_by(access_code=db_access_code).first()
            else:
                print(f"❌ 缺少team_id和access_code")
                return jsonify({"error": "请提供team_id或access_code"}), 400
            
            if not team:
                print(f"❌ 未找到团队")
                return jsonify({"error": "未找到团队"}), 404
            
            print(f"✅ 找到团队: {team.team_name} (ID: {team.id})")
            
            # 将变量和规则保存到ai_chat_notes字段（JSON格式）
            import json
            notes_data = {}
            try:
                if team.ai_chat_notes:
                    notes_data = json.loads(team.ai_chat_notes)
            except:
                notes_data = {}
            
            notes_data["variables"] = variables
            notes_data["rules"] = rules
            notes_data["submitted_at"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            
            team.ai_chat_notes = json.dumps(notes_data, ensure_ascii=False)
            team.updated_at = datetime.now()
            db.session.commit()
            
            print(f"✅ 变量和规则已保存到数据库")
            print(f"{'='*60}\n")
            
            # 使缓存失效
            invalidate_teams_cache()
            
            return jsonify({
                "success": True,
                "message": "变量和规则已保存"
            })
    except Exception as e:
        print(f"❌ 保存变量和规则失败: {e}")
        import traceback
        traceback.print_exc()
        print(f"{'='*60}\n")
        return jsonify({"error": f"保存失败: {str(e)}"}), 500


@app.route("/api/teams/by-access-code", methods=["POST"])
def get_team_by_access_code():
    """根据访问码查询团队信息"""
    data = request.get_json() or {}
    access_code_input = data.get("access_code", "").strip()
    
    if not access_code_input:
        return jsonify({"error": "请输入访问码"}), 400
    
    # 将学生输入的访问码转换为数据库格式
    # 例如：51101 -> team_511_01
    try:
        # 假设格式：前3位是班级编号，后2位是团队序号
        if len(access_code_input) == 5:
            class_num = access_code_input[:3]
            team_seq = access_code_input[3:]
            db_access_code = f"team_{class_num}_{team_seq}"
        else:
            return jsonify({"error": "访问码格式不正确，应为5位数字（如：51101）"}), 400
    except Exception as e:
        return jsonify({"error": f"访问码解析失败: {str(e)}"}), 400
    
    # 从数据库查询团队
    with app.app_context():
        try:
            if USE_DATABASE and DB_AVAILABLE:
                team = Team.query.filter_by(access_code=db_access_code).first()
                if team:
                    team_dict = team.to_dict()
                    # 找到队长
                    leader = None
                    if team.members:
                        for member in team.members:
                            if member.is_leader:
                                leader = member.name
                                break
                    
                    # 获取游戏名称和简介（优先使用AI生成的）
                    game_name = team_dict.get("ai_game_name") or team_dict.get("game_name", "待定游戏")
                    game_desc = team_dict.get("ai_game_desc") or team_dict.get("game_desc", "")
                    
                    return jsonify({
                        "success": True,
                        "team": {
                            "id": team_dict.get("id"),
                            "team_name": team_dict.get("team_name"),
                            "class_name": team_dict.get("class_name"),
                            "game_name": game_name,
                            "game_desc": game_desc,
                            "leader": leader,
                            "members": team_dict.get("members", [])
                        }
                    })
            else:
                # 从文件读取
                teams = load_teams()
                team = next((t for t in teams if t.get("access_code") == db_access_code), None)
                if team:
                    # 找到队长
                    leader = None
                    members = team.get("members", [])
                    for member in members:
                        if member.get("is_leader"):
                            leader = member.get("name")
                            break
                    
                    # 获取游戏名称和简介（优先使用AI生成的）
                    game_name = team.get("ai_game_name") or team.get("game_name", "待定游戏")
                    game_desc = team.get("ai_game_desc") or team.get("game_desc", "")
                    
                    return jsonify({
                        "success": True,
                        "team": {
                            "id": team.get("id"),
                            "team_name": team.get("team_name"),
                            "class_name": team.get("class_name"),
                            "game_name": game_name,
                            "game_desc": game_desc,
                            "leader": leader,
                            "members": members
                        }
                    })
            
            return jsonify({"error": "未找到对应的团队，请检查访问码是否正确"}), 404
        except Exception as e:
            print(f"查询团队失败: {e}")
            import traceback
            traceback.print_exc()
            return jsonify({"error": f"查询失败: {str(e)}"}), 500


@app.route("/api/teams/by-class", methods=["POST"])
def get_teams_by_class():
    """根据班级编号或班级名称获取该班级的所有团队信息"""
    data = request.get_json() or {}
    class_input = data.get("class_name", "").strip()
    
    if not class_input:
        return jsonify({"error": "请输入班级编号或班级名称"}), 400
    
    teams = load_teams()
    
    # 支持通过班级编号（如"511"）或完整班级名称（如"五年级11班"）查询
    class_teams = []
    import re
    
    if class_input.isdigit():
        # 如果是纯数字，尝试匹配班级名称
        # 例如：511 匹配 "五年级11班"（提取出5和11，组合为511）
        for team in teams:
            class_name = team.get("class_name", "")
            if not class_name:
                continue
                
            # 提取班级名称中的数字部分
            class_nums = re.findall(r'\d+', class_name)
            
            if class_nums:
                matched = False
                
                if len(class_nums) >= 2:
                    # 年级+班级：如"五年级11班" -> ["5", "11"] -> "511"
                    # 注意：班级号可能需要补零，如"五年级1班" -> ["5", "1"] -> "501"
                    grade_num = class_nums[0]
                    class_num = class_nums[1].zfill(2)  # 班级号补零到2位
                    combined = grade_num + class_num
                    
                    if combined == class_input:
                        matched = True
                    # 也尝试不补零的情况
                    elif grade_num + class_nums[1] == class_input:
                        matched = True
                
                elif len(class_nums) == 1:
                    # 只有班级号：尝试匹配后2位
                    if len(class_input) >= 2:
                        if class_nums[0] == class_input[-2:] or class_nums[0] == class_input[-1:]:
                            matched = True
                
                # 也检查完整班级名称是否包含输入的数字
                if not matched and class_input in class_name:
                    matched = True
                
                if matched:
                    class_teams.append(team)
    else:
        # 如果是完整班级名称，直接匹配
        class_teams = [t for t in teams if t.get("class_name") == class_input]
    
    # 转换访问码格式并获取完整团队信息
    teams_with_info = []
    for team in class_teams:
        access_code = team.get("access_code", "")
        # 转换为5位数字格式
        display_code = ""
        if access_code.startswith("team_"):
            # 提取 team_511_01 中的数字部分
            parts = access_code.replace("team_", "").split("_")
            if len(parts) == 2:
                display_code = parts[0] + parts[1]
        elif len(access_code) == 5 and access_code.isdigit():
            # 如果已经是5位数字，直接使用
            display_code = access_code
        else:
            # 如果格式不对，尝试提取数字
            import re
            nums = re.findall(r'\d+', access_code)
            if nums:
                display_code = ''.join(nums)[:5].zfill(5)
        
        # 找到队长
        leader_name = ""
        members = team.get("members", [])
        for member in members:
            if isinstance(member, dict) and member.get("is_leader"):
                leader_name = member.get("name", "")
                break
        
        # 获取游戏名称和简介（优先使用AI生成的，否则使用原始）
        game_name = team.get("ai_game_name") or team.get("game_name", "待定游戏")
        game_desc = team.get("ai_game_desc") or team.get("game_desc", "")
        
        teams_with_info.append({
            "id": team.get("id"),
            "team_name": team.get("team_name", ""),
            "leader_name": leader_name,
            "game_name": game_name,
            "game_desc": game_desc,
            "access_code": access_code,  # 原始访问码
            "display_code": display_code,  # 5位数字格式的访问码
            "members": members
        })
    
    # 获取实际匹配的班级名称（用于显示）
    actual_class_name = ""
    if class_teams:
        actual_class_name = class_teams[0].get("class_name", "")
    
    return jsonify({
        "success": True,
        "class_name": actual_class_name,
        "teams": teams_with_info,
        "count": len(teams_with_info)
    })


@app.route("/api/admin/classes", methods=["GET"])
def admin_classes():
    ensure_students_loaded()
    return jsonify({"classes": CLASS_LIST})


@app.route("/api/admin/students", methods=["GET"])
def admin_students():
    class_name = request.args.get("class_name")
    if not class_name:
        # 返回所有学生
        teams = load_teams()
        all_teamed_students = set()
        for team in teams:
            for member in team.get("members", []):
                all_teamed_students.add(member.get("name", ""))
        
        ensure_students_loaded()
        return jsonify(
            {
                "class_name": "全年级",
                "all_students": ALL_STUDENTS,
                "available_students": [s for s in ALL_STUDENTS if s["name"] not in all_teamed_students],
                "teamed_students": list(all_teamed_students),
                "total_count": len(ALL_STUDENTS),
                "available_count": len(ALL_STUDENTS) - len(all_teamed_students),
            }
        )

    class_students = get_class_students(class_name)
    teams = load_teams()
    teamed_students = get_teamed_students(teams, class_name)
    available_students = [s for s in class_students if s["name"] not in teamed_students]

    return jsonify(
        {
            "class_name": class_name,
            "all_students": class_students,
            "available_students": available_students,
            "teamed_students": list(teamed_students),
            "total_count": len(class_students),
            "available_count": len(available_students),
        }
    )


@app.route("/api/admin/teams", methods=["GET"])
def admin_get_teams():
    class_name = request.args.get("class_name")
    teams = load_teams()
    if class_name:
        # 返回指定班级的团队
        class_teams = [t for t in teams if t.get("class_name") == class_name]
        return jsonify({"teams": class_teams, "count": len(class_teams)})
    else:
        # 返回所有团队的统计
        return jsonify({"teams": teams, "count": len(teams)})


@app.route("/api/admin/teams/<int:team_id>/dissolve", methods=["POST"])
def admin_dissolve_team(team_id):
    class_name = request.get_json(silent=True, force=False) or {}
    class_name = class_name.get("class_name") or request.args.get("class_name")
    if not class_name:
        return jsonify({"error": "class_name 必填"}), 400

    with app.app_context():
        team = Team.query.filter_by(id=team_id, class_name=class_name).first()
        if not team:
            return jsonify({"error": "未找到该团队"}), 404
        
        db.session.delete(team)  # 级联删除团队成员和对话
        db.session.commit()
        # 使缓存失效
        invalidate_teams_cache()
    
    return jsonify({"success": True})


@app.route("/api/admin/teams/<int:team_id>/add-member", methods=["POST"])
def admin_add_member(team_id):
    data = request.get_json() or {}
    class_name = data.get("class_name")
    student_name = data.get("student_name", "").strip()
    if not class_name or not student_name:
        return jsonify({"error": "class_name 与 student_name 必填"}), 400

    class_students = get_class_students(class_name)
    if not any(s["name"] == student_name for s in class_students):
        return jsonify({"error": "学生不在该班级"}), 400

    # 检查学生是否已在其他团队
    with app.app_context():
        # 查询该班级所有团队
        existing_teams = Team.query.filter_by(class_name=class_name).all()
        for team in existing_teams:
            if team.id == team_id:
                continue
            for member in team.members:
                if member.name == student_name:
                    return jsonify({"error": "该学生已在其他团队"}), 400
        
        # 添加成员到指定团队
        team = Team.query.filter_by(id=team_id, class_name=class_name).first()
        if not team:
            return jsonify({"error": "未找到该团队"}), 404
        
        # 检查是否已经是成员
        existing_member = TeamMember.query.filter_by(team_id=team_id, name=student_name).first()
        if existing_member:
            return jsonify({"error": "该学生已是该团队成员"}), 400
        
        member = TeamMember(
            team_id=team_id,
            name=student_name,
            is_leader=False
        )
        db.session.add(member)
        db.session.commit()
        # 使缓存失效
        invalidate_teams_cache()
    
    return jsonify({"success": True})


@app.route("/api/admin/teams", methods=["POST"])
def admin_create_team():
    """教师端创建新团队"""
    data = request.get_json() or {}
    class_name = data.get("class_name")
    team_name = data.get("team_name", "").strip()
    members = data.get("members", [])  # 成员列表，格式：[{"name": "xxx", "is_leader": true/false}]
    
    if not class_name:
        return jsonify({"error": "class_name 必填"}), 400
    
    if not team_name:
        return jsonify({"error": "团队名称不能为空"}), 400
    
    if not members or len(members) < 1:
        return jsonify({"error": "至少需要一个成员"}), 400
    
    # 验证成员是否都在该班级
    class_students = get_class_students(class_name)
    class_student_names = {s["name"] for s in class_students}
    
    for member in members:
        if member.get("name") not in class_student_names:
            return jsonify({"error": f"学生 {member.get('name')} 不在该班级"}), 400
    
    teams = load_teams()
    teamed_students = get_teamed_students(teams, class_name)
    
    # 检查成员是否已在其他团队
    for member in members:
        if member.get("name") in teamed_students:
            return jsonify({"error": f"学生 {member.get('name')} 已在其他团队"}), 400
    
    # 确保至少有一个队长
    has_leader = any(m.get("is_leader", False) for m in members)
    if not has_leader:
        members[0]["is_leader"] = True
    
    # 生成访问码
    teams = load_teams()
    access_code = generate_access_code(class_name, teams)
    
    # 创建新团队（直接保存到数据库）
    team_id = int(time.time() * 1000)
    with app.app_context():
        team = Team(
            id=team_id,
            class_name=class_name,
            team_name=team_name,
            game_name="待定游戏",
            game_desc="",
            access_code=access_code,
            submitter=members[0].get("name", ""),
            created_at=datetime.now()
        )
        db.session.add(team)
        
        # 添加团队成员
        for member_data in members:
            member = TeamMember(
                team_id=team_id,
                name=member_data.get("name", ""),
                is_leader=member_data.get("is_leader", False)
            )
            db.session.add(member)
        
        db.session.commit()
        # 使缓存失效
        invalidate_teams_cache()
    
    # 返回团队数据（兼容格式）
    new_team = {
        "id": team_id,
        "team_name": team_name,
        "class_name": class_name,
        "members": members,
        "game_name": "待定游戏",
        "game_desc": "",
        "access_code": access_code,
        "submitter": members[0].get("name", ""),
        "created_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }
    
    return jsonify({"success": True, "team": new_team})


@app.route("/api/admin/statistics", methods=["GET"])
def admin_statistics():
    """获取全年级统计数据"""
    teams = load_teams()
    
    # 按班级统计
    class_stats = {}
    for team in teams:
        class_name = team.get("class_name", "未知班级")
        if class_name not in class_stats:
            class_stats[class_name] = {
                "class_name": class_name,
                "team_count": 0,
                "student_count": 0,
                "teamed_students": set()
            }
        
        class_stats[class_name]["team_count"] += 1
        members = team.get("members", [])
        for member in members:
            class_stats[class_name]["teamed_students"].add(member.get("name", ""))
    
    # 转换为列表格式
    stats_list = []
    total_teams = 0
    total_students = 0
    
    for class_name, stats in class_stats.items():
        student_count = len(stats["teamed_students"])
        stats_list.append({
            "class_name": class_name,
            "team_count": stats["team_count"],
            "student_count": student_count
        })
        total_teams += stats["team_count"]
        total_students += student_count
    
    return jsonify({
        "total_teams": total_teams,
        "total_students": total_students,
        "class_statistics": stats_list
    })


@app.route("/api/teams/<int:team_id>/conversations/save", methods=["POST"])
def save_team_conversations(team_id):
    """保存团队的对话数据到数据库（学生端可调用）"""
    try:
        with app.app_context():
            team_obj = Team.query.filter_by(id=team_id).first()
            if not team_obj:
                return jsonify({"error": "未找到该团队"}), 404
            team = team_obj.to_dict()
        
        access_code = team.get("access_code")
        if not access_code:
            return jsonify({"error": "团队没有访问码"}), 400
        
        # 使用JWT获取access_token，然后调用Coze API获取对话数据
        try:
            # 生成JWT Token
            jwt_token = generate_coze_jwt_token(
                team_id=team.get("id"),
                class_name=team.get("class_name"),
                team_name=team.get("team_name"),
                access_code=access_code
            )
            
            if not jwt_token:
                return jsonify({"error": "JWT Token生成失败"}), 500
            
            # 获取access_token
            access_token = get_coze_access_token(jwt_token)
            if not access_token:
                return jsonify({"error": "获取access_token失败"}), 500
            
            # 调用Coze API获取会话列表
            session_name = access_code
            
            # 获取会话列表
            url = "https://api.coze.cn/v1/conversations"
            headers = {
                "Authorization": f"Bearer {access_token}",
                "Content-Type": "application/json"
            }
            params = {
                "bot_id": COZE_APP_ID,
                "limit": 100
            }
            
            response = requests.get(url, headers=headers, params=params, timeout=10)
            
            if response.status_code != 200:
                return jsonify({
                    "error": "获取会话列表失败",
                    "status_code": response.status_code,
                    "message": response.text
                }), response.status_code
            
            conversations = response.json().get("data", {}).get("conversations", [])
            
            # 获取每个会话的消息
            conversations_with_messages = []
            for conv in conversations[:10]:  # 限制最多10个会话
                conv_id = conv.get("id")
                if not conv_id:
                    continue
                
                # 获取会话消息
                messages_url = f"https://api.coze.cn/v1/conversations/{conv_id}/messages"
                messages_response = requests.get(messages_url, headers=headers, params={"limit": 50}, timeout=10)
                
                messages = []
                if messages_response.status_code == 200:
                    messages_data = messages_response.json().get("data", {})
                    messages = messages_data.get("messages", [])
                
                conversations_with_messages.append({
                    "conversation_id": conv_id,
                    "conversation_name": conv.get("name", "未命名会话"),
                    "created_at": conv.get("created_at"),
                    "message_count": len(messages),
                    "messages": messages[:20]  # 限制每条会话最多20条消息
                })
            
            # 保存对话数据到本地数据库
            conversations_data = {
                "team_id": team_id,
                "team_name": team.get("team_name"),
                "class_name": team.get("class_name"),
                "access_code": access_code,
                "session_name": session_name,
                "conversations": conversations_with_messages,
                "total_conversations": len(conversations),
                "saved_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            }
            save_conversations(team_id, conversations_data)
            
            return jsonify({
                "success": True,
                "message": "对话已保存",
                "team_id": team_id,
                "total_conversations": len(conversations),
                "saved_at": conversations_data["saved_at"]
            })
            
        except Exception as e:
            print(f"保存对话数据失败: {e}")
            import traceback
            traceback.print_exc()
            return jsonify({"error": f"保存对话数据失败: {str(e)}"}), 500
            
    except Exception as e:
        print(f"保存对话失败: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({"error": f"保存失败: {str(e)}"}), 500


@app.route("/api/admin/teams/<int:team_id>/conversations", methods=["GET"])
def admin_get_conversations(team_id):
    """获取团队的对话数据"""
    class_name = request.args.get("class_name")
    if not class_name:
        return jsonify({"error": "class_name 必填"}), 400
    
    with app.app_context():
        team_obj = Team.query.filter_by(id=team_id, class_name=class_name).first()
        if not team_obj:
            return jsonify({"error": "未找到该团队"}), 404
        team = team_obj.to_dict()
    
    access_code = team.get("access_code")
    if not access_code:
        return jsonify({"error": "团队没有访问码"}), 400
    
    # 使用JWT获取access_token，然后调用Coze API获取对话数据
    try:
        # 生成JWT Token
        jwt_token = generate_coze_jwt_token(
            team_id=team.get("id"),
            class_name=team.get("class_name"),
            team_name=team.get("team_name"),
            access_code=access_code
        )
        
        if not jwt_token:
            return jsonify({"error": "JWT Token生成失败"}), 500
        
        # 获取access_token
        access_token = get_coze_access_token(jwt_token)
        if not access_token:
            return jsonify({"error": "获取access_token失败"}), 500
        
        # 调用Coze API获取会话列表
        # 注意：session_name就是access_code
        session_name = access_code
        
        # 获取会话列表
        url = "https://api.coze.cn/v1/conversations"
        headers = {
            "Authorization": f"Bearer {access_token}",
            "Content-Type": "application/json"
        }
        params = {
            "bot_id": COZE_APP_ID,
            "limit": 100
        }
        
        response = requests.get(url, headers=headers, params=params, timeout=10)
        
        if response.status_code != 200:
            return jsonify({
                "error": "获取会话列表失败",
                "status_code": response.status_code,
                "message": response.text
            }), response.status_code
        
        conversations = response.json().get("data", {}).get("conversations", [])
        
        # 筛选出与当前session_name相关的会话
        # 注意：Coze API可能不直接支持按session_name筛选，需要根据实际情况调整
        # 这里先返回所有会话，前端可以根据需要筛选
        
        # 获取每个会话的消息
        conversations_with_messages = []
        for conv in conversations[:10]:  # 限制最多10个会话
            conv_id = conv.get("id")
            if not conv_id:
                continue
            
            # 获取会话消息
            messages_url = f"https://api.coze.cn/v1/conversations/{conv_id}/messages"
            messages_response = requests.get(messages_url, headers=headers, params={"limit": 50}, timeout=10)
            
            messages = []
            if messages_response.status_code == 200:
                messages_data = messages_response.json().get("data", {})
                messages = messages_data.get("messages", [])
            
            conversations_with_messages.append({
                "conversation_id": conv_id,
                "conversation_name": conv.get("name", "未命名会话"),
                "created_at": conv.get("created_at"),
                "message_count": len(messages),
                "messages": messages[:20]  # 限制每条会话最多20条消息
            })
        
        # 保存对话数据到本地
        conversations_data = {
            "team_id": team_id,
            "team_name": team.get("team_name"),
            "class_name": team.get("class_name"),
            "access_code": access_code,
            "session_name": session_name,
            "conversations": conversations_with_messages,
            "total_conversations": len(conversations),
            "saved_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        }
        save_conversations(team_id, conversations_data)
        
        return jsonify({
            "success": True,
            "team_id": team_id,
            "team_name": team.get("team_name"),
            "access_code": access_code,
            "session_name": session_name,
            "conversations": conversations_with_messages,
            "total_conversations": len(conversations)
        })
        
    except Exception as e:
        print(f"获取对话数据失败: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({"error": f"获取对话数据失败: {str(e)}"}), 500


@app.route("/api/admin/teams/<int:team_id>/conversations/download", methods=["GET"])
def admin_download_conversations(team_id):
    """下载团队的对话数据Word文档"""
    class_name = request.args.get("class_name")
    if not class_name:
        return jsonify({"error": "class_name 必填"}), 400
    
    with app.app_context():
        team_obj = Team.query.filter_by(id=team_id, class_name=class_name).first()
        if not team_obj:
            return jsonify({"error": "未找到该团队"}), 404
        team = team_obj.to_dict()
    
    try:
        # 尝试从数据库加载对话数据
        conversations_data = load_conversations(team_id)
        
        # 如果数据库中没有，尝试从API获取
        if not conversations_data:
            # 调用获取对话的API逻辑（简化版）
            access_code = team.get("access_code")
            if not access_code:
                return jsonify({"error": "团队没有访问码"}), 400
            
            jwt_token = generate_coze_jwt_token(
                team_id=team.get("id"),
                class_name=team.get("class_name"),
                team_name=team.get("team_name"),
                access_code=access_code
            )
            
            if not jwt_token:
                return jsonify({"error": "JWT Token生成失败"}), 500
            
            access_token = get_coze_access_token(jwt_token)
            if not access_token:
                return jsonify({"error": "获取access_token失败"}), 500
            
            # 获取会话列表
            url = "https://api.coze.cn/v1/conversations"
            headers = {
                "Authorization": f"Bearer {access_token}",
                "Content-Type": "application/json"
            }
            params = {
                "bot_id": COZE_APP_ID,
                "limit": 100
            }
            
            response = requests.get(url, headers=headers, params=params, timeout=10)
            if response.status_code != 200:
                return jsonify({"error": "获取会话列表失败"}), response.status_code
            
            conversations = response.json().get("data", {}).get("conversations", [])
            conversations_with_messages = []
            
            for conv in conversations[:10]:
                conv_id = conv.get("id")
                if not conv_id:
                    continue
                
                messages_url = f"https://api.coze.cn/v1/conversations/{conv_id}/messages"
                messages_response = requests.get(messages_url, headers=headers, params={"limit": 50}, timeout=10)
                
                messages = []
                if messages_response.status_code == 200:
                    messages_data = messages_response.json().get("data", {})
                    messages = messages_data.get("messages", [])
                
                conversations_with_messages.append({
                    "conversation_id": conv_id,
                    "conversation_name": conv.get("name", "未命名会话"),
                    "created_at": conv.get("created_at"),
                    "message_count": len(messages),
                    "messages": messages[:20]
                })
            
            conversations_data = {
                "team_id": team_id,
                "team_name": team.get("team_name"),
                "class_name": team.get("class_name"),
                "access_code": access_code,
                "conversations": conversations_with_messages,
                "total_conversations": len(conversations),
                "saved_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            }
            
            # 保存到本地
            save_conversations(team_id, conversations_data)
        
        # 生成Word文档
        doc = generate_word_document(team, conversations_data)
        
        # 保存到内存
        doc_stream = BytesIO()
        doc.save(doc_stream)
        doc_stream.seek(0)
        
        # 生成文件名
        team_name_safe = re.sub(r'[^\w\s-]', '', team.get("team_name", "团队")).strip()
        filename = f"{team_name_safe}_对话记录_{datetime.now().strftime('%Y%m%d_%H%M%S')}.docx"
        
        return send_file(
            doc_stream,
            mimetype='application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            as_attachment=True,
            download_name=filename
        )
        
    except Exception as e:
        print(f"生成Word文档失败: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({"error": f"生成Word文档失败: {str(e)}"}), 500


@app.route("/api/admin/teams/export-excel", methods=["GET"])
def admin_export_teams_excel():
    """导出团队数据为Excel文件"""
    class_name = request.args.get("class_name")
    
    teams = load_teams()
    if class_name:
        teams = [t for t in teams if t.get("class_name") == class_name]
    
    try:
        # 创建Excel工作簿
        wb = Workbook()
        ws = wb.active
        ws.title = "团队记录表"
        
        # 设置表头样式
        header_fill = PatternFill(start_color="4472C4", end_color="4472C4", fill_type="solid")
        header_font = Font(bold=True, color="FFFFFF", size=12)
        header_alignment = Alignment(horizontal="center", vertical="center", wrap_text=True)
        border = Border(
            left=Side(style='thin'),
            right=Side(style='thin'),
            top=Side(style='thin'),
            bottom=Side(style='thin')
        )
        
        # 设置列标题
        headers = ['团队名称', '班级', '提交人', '队长', '队员', '游戏名称', '游戏简介', '访问码', '创建时间']
        for col_idx, header in enumerate(headers, 1):
            cell = ws.cell(row=1, column=col_idx, value=header)
            cell.fill = header_fill
            cell.font = header_font
            cell.alignment = header_alignment
            cell.border = border
        
        # 设置列宽
        column_widths = [20, 15, 12, 12, 30, 20, 40, 15, 18]
        for col_idx, width in enumerate(column_widths, 1):
            ws.column_dimensions[ws.cell(row=1, column=col_idx).column_letter].width = width
        
        # 填充数据
        for row_idx, team in enumerate(teams, 2):
            leader = next((m for m in team.get("members", []) if m.get("is_leader")), None)
            members = [m.get("name", "") for m in team.get("members", [])]
            members_str = "、".join(members)
            
            row_data = [
                team.get("team_name", ""),
                team.get("class_name", ""),
                team.get("submitter", ""),
                leader.get("name", "") if leader else "",
                members_str,
                team.get("ai_game_name") or team.get("game_name", "待定游戏"),
                team.get("ai_game_desc") or team.get("game_desc", ""),
                team.get("access_code", ""),
                team.get("created_at", "")
            ]
            
            for col_idx, value in enumerate(row_data, 1):
                cell = ws.cell(row=row_idx, column=col_idx, value=value)
                cell.border = border
                cell.alignment = Alignment(vertical="top", wrap_text=True)
                
                # 游戏简介列左对齐，其他列居中
                if col_idx == 7:  # 游戏简介列
                    cell.alignment = Alignment(horizontal="left", vertical="top", wrap_text=True)
                else:
                    cell.alignment = Alignment(horizontal="center", vertical="top", wrap_text=True)
        
        # 设置行高
        for row in ws.iter_rows(min_row=2):
            ws.row_dimensions[row[0].row].height = 30
        
        # 保存到内存
        excel_stream = BytesIO()
        wb.save(excel_stream)
        excel_stream.seek(0)
        
        # 生成文件名
        if class_name:
            filename = f"{class_name}_团队记录表_{datetime.now().strftime('%Y%m%d_%H%M%S')}.xlsx"
        else:
            filename = f"全年级_团队记录表_{datetime.now().strftime('%Y%m%d_%H%M%S')}.xlsx"
        
        return send_file(
            excel_stream,
            mimetype='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            as_attachment=True,
            download_name=filename
        )
        
    except Exception as e:
        print(f"导出Excel失败: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({"error": f"导出Excel失败: {str(e)}"}), 500


# ========== Coze 智能体数据接口 ==========
# 这些接口专门为Coze智能体设计，返回JSON格式数据

@app.route("/api/coze/teams", methods=["GET"])
def coze_get_teams():
    """
    Coze智能体接口：获取所有团队数据
    无需登录验证，供Coze智能体调用
    """
    try:
        teams = load_teams()
        # 格式化数据供Coze使用
        formatted_teams = []
        for team in teams:
            # 找到队长
            leader = None
            members = team.get("members", [])
            for member in members:
                if isinstance(member, dict) and (member.get("is_leader") or member.get("isLeader")):
                    leader = member.get("name")
                    break
            
            formatted_teams.append({
                "team_id": team.get("id"),
                "team_name": team.get("team_name", ""),
                "class_name": team.get("class_name", ""),
                "game_name": team.get("ai_game_name") or team.get("game_name", "待定游戏"),
                "game_desc": team.get("ai_game_desc") or team.get("game_desc", ""),
                "leader": leader,
                "members": [m.get("name") if isinstance(m, dict) else m for m in members],
                "access_code": team.get("access_code", ""),
                "created_at": team.get("created_at", "")
            })
        
        return jsonify({
            "success": True,
            "count": len(formatted_teams),
            "teams": formatted_teams
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/api/coze/teams/<int:team_id>", methods=["GET"])
def coze_get_team(team_id):
    """
    Coze智能体接口：获取单个团队详细信息
    """
    try:
        teams = load_teams()
        team = next((t for t in teams if t.get("id") == team_id), None)
        
        if not team:
            return jsonify({"success": False, "error": "团队不存在"}), 404
        
        # 找到队长
        leader = None
        members = team.get("members", [])
        for member in members:
            if isinstance(member, dict) and (member.get("is_leader") or member.get("isLeader")):
                leader = member.get("name")
                break
        
        return jsonify({
            "success": True,
            "team": {
                "team_id": team.get("id"),
                "team_name": team.get("team_name", ""),
                "class_name": team.get("class_name", ""),
                "game_name": team.get("ai_game_name") or team.get("game_name", "待定游戏"),
                "game_desc": team.get("ai_game_desc") or team.get("game_desc", ""),
                "game_notes": team.get("ai_chat_notes", ""),
                "leader": leader,
                "members": [m.get("name") if isinstance(m, dict) else m for m in members],
                "access_code": team.get("access_code", ""),
                "created_at": team.get("created_at", "")
            }
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/api/coze/teams/by-class", methods=["GET"])
def coze_get_teams_by_class():
    """
    Coze智能体接口：根据班级获取团队列表
    参数：class_name (查询参数)
    """
    try:
        class_name = request.args.get("class_name", "").strip()
        if not class_name:
            return jsonify({"success": False, "error": "请提供班级名称"}), 400
        
        teams = load_teams()
        class_teams = [t for t in teams if t.get("class_name") == class_name]
        
        formatted_teams = []
        for team in class_teams:
            leader = None
            members = team.get("members", [])
            for member in members:
                if isinstance(member, dict) and (member.get("is_leader") or member.get("isLeader")):
                    leader = member.get("name")
                    break
            
            formatted_teams.append({
                "team_id": team.get("id"),
                "team_name": team.get("team_name", ""),
                "game_name": team.get("ai_game_name") or team.get("game_name", "待定游戏"),
                "game_desc": team.get("ai_game_desc") or team.get("game_desc", ""),
                "leader": leader,
                "members": [m.get("name") if isinstance(m, dict) else m for m in members],
                "access_code": team.get("access_code", "")
            })
        
        return jsonify({
            "success": True,
            "class_name": class_name,
            "count": len(formatted_teams),
            "teams": formatted_teams
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/api/coze/students", methods=["GET"])
def coze_get_students():
    """
    Coze智能体接口：获取所有学生数据
    """
    try:
        ensure_students_loaded()
        return jsonify({
            "success": True,
            "count": len(ALL_STUDENTS),
            "students": ALL_STUDENTS
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/api/coze/classes", methods=["GET"])
def coze_get_classes():
    """
    Coze智能体接口：获取所有班级列表
    """
    try:
        ensure_students_loaded()
        return jsonify({
            "success": True,
            "count": len(CLASS_LIST),
            "classes": CLASS_LIST
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/api/coze/statistics", methods=["GET"])
def coze_get_statistics():
    """
    Coze智能体接口：获取统计数据
    """
    try:
        ensure_students_loaded()
        teams = load_teams()
        
        # 按班级统计
        class_stats = {}
        for team in teams:
            class_name = team.get("class_name", "未知班级")
            if class_name not in class_stats:
                class_stats[class_name] = {
                    "class_name": class_name,
                    "team_count": 0,
                    "student_count": 0,
                    "teamed_students": set()
                }
            
            class_stats[class_name]["team_count"] += 1
            members = team.get("members", [])
            for member in members:
                member_name = member.get("name") if isinstance(member, dict) else member
                class_stats[class_name]["teamed_students"].add(member_name)
        
        # 转换为列表格式
        stats_list = []
        for class_name, stats in class_stats.items():
            student_count = len(stats["teamed_students"])
            stats_list.append({
                "class_name": class_name,
                "team_count": stats["team_count"],
                "student_count": student_count
            })
        
        return jsonify({
            "success": True,
            "total_teams": len(teams),
            "total_students": len(ALL_STUDENTS),
            "class_statistics": stats_list
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


# 挂到学习空间「项目化学习」时使用前缀，单独打开时留空即可
GAME_PREFIX = os.environ.get("GAME_PREFIX", "").strip().rstrip("/")
_PREFIX_RE = re.compile(
    r'(["\'`])(/(?:api|logout|login|teacher|second|game-rules|ai-chat|ai-study|further-learning|team|index)(?:[^"\'`\\]*)?)'
)


def _prefix_path(path):
    if not GAME_PREFIX or not path.startswith("/"):
        return path
    if path == GAME_PREFIX or path.startswith(GAME_PREFIX + "/"):
        return path
    return GAME_PREFIX + path


@app.after_request
def _apply_game_prefix(resp):
    if not GAME_PREFIX:
        return resp
    loc = resp.headers.get("Location")
    if loc and loc.startswith("/") and not loc.startswith(GAME_PREFIX):
        resp.headers["Location"] = _prefix_path(loc)
    ctype = resp.headers.get("Content-Type", "")
    if "text/html" not in ctype:
        return resp
    body = resp.get_data(as_text=True)

    def repl(match):
        return match.group(1) + _prefix_path(match.group(2))

    new_body = _PREFIX_RE.sub(repl, body)
    if new_body != body:
        resp.set_data(new_body)
    return resp


class _PrefixMiddleware(object):
    def __init__(self, wsgi_app, prefix):
        self.wsgi_app = wsgi_app
        self.prefix = prefix

    def __call__(self, environ, start_response):
        path = environ.get("PATH_INFO", "") or "/"
        prefix = self.prefix
        if path == prefix or path.startswith(prefix + "/"):
            environ["SCRIPT_NAME"] = (environ.get("SCRIPT_NAME") or "") + prefix
            rest = path[len(prefix):] or "/"
            environ["PATH_INFO"] = rest
        return self.wsgi_app(environ, start_response)


if GAME_PREFIX:
    app.wsgi_app = _PrefixMiddleware(app.wsgi_app, GAME_PREFIX)


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5004))
    app.run(debug=True, host="0.0.0.0", port=port)



