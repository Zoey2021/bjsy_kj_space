"""
数据库模型定义
使用 SQLAlchemy ORM
"""
from flask_sqlalchemy import SQLAlchemy
from datetime import datetime
import json

db = SQLAlchemy()


class Student(db.Model):
    """学生表"""
    __tablename__ = 'students'
    
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    class_name = db.Column(db.String(50), nullable=False, index=True)
    student_id = db.Column(db.Integer, nullable=False)
    name = db.Column(db.String(50), nullable=False)
    avatar = db.Column(db.String(10), default='👤')
    
    # 唯一约束：同一班级内学号唯一
    __table_args__ = (db.UniqueConstraint('class_name', 'student_id', name='uq_class_student'),)
    
    def to_dict(self):
        return {
            'id': self.id,
            'class_name': self.class_name,
            'student_id': self.student_id,
            'name': self.name,
            'avatar': self.avatar
        }


class Team(db.Model):
    """团队表"""
    __tablename__ = 'teams'
    
    id = db.Column(db.BigInteger, primary_key=True)
    class_name = db.Column(db.String(50), nullable=False, index=True)
    team_name = db.Column(db.String(100), nullable=False)
    game_name = db.Column(db.String(100), default='待定游戏')
    game_desc = db.Column(db.Text, default='')
    ai_game_name = db.Column(db.String(100), default='')
    ai_game_desc = db.Column(db.Text, default='')
    ai_chat_notes = db.Column(db.Text, default='')
    access_code = db.Column(db.String(50), unique=True, nullable=False, index=True)
    submitter = db.Column(db.String(50), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.now, nullable=False)
    updated_at = db.Column(db.DateTime, default=datetime.now, onupdate=datetime.now)
    
    # 关联关系
    members = db.relationship('TeamMember', backref='team', lazy=True, cascade='all, delete-orphan')
    
    def to_dict(self):
        """转换为字典格式（兼容原有JSON格式）"""
        try:
            members_list = []
            if self.members:
                try:
                    members_list = [m.to_dict() for m in self.members]
                except Exception as e:
                    print(f"⚠️  转换团队成员失败: {e}")
                    members_list = []
            
            created_at_str = None
            if self.created_at:
                try:
                    created_at_str = self.created_at.strftime('%Y-%m-%d %H:%M:%S')
                except Exception as e:
                    print(f"⚠️  格式化创建时间失败: {e}")
                    created_at_str = str(self.created_at)
            
            return {
                'id': self.id,
                'class_name': self.class_name or '',
                'team_name': self.team_name or '',
                'game_name': self.game_name or '待定游戏',
                'game_desc': self.game_desc or '',
                'ai_game_name': self.ai_game_name or None,
                'ai_game_desc': self.ai_game_desc or None,
                'ai_chat_notes': self.ai_chat_notes or None,
                'members': members_list,
                'created_at': created_at_str,
                'submitter': self.submitter or '',
                'access_code': self.access_code or ''
            }
        except Exception as e:
            print(f"❌ Team.to_dict() 失败: {e}")
            import traceback
            traceback.print_exc()
            # 返回最小可用字典
            return {
                'id': getattr(self, 'id', None),
                'class_name': getattr(self, 'class_name', ''),
                'team_name': getattr(self, 'team_name', ''),
                'game_name': getattr(self, 'game_name', '待定游戏'),
                'game_desc': getattr(self, 'game_desc', ''),
                'ai_game_name': getattr(self, 'ai_game_name', None),
                'ai_game_desc': getattr(self, 'ai_game_desc', None),
                'ai_chat_notes': getattr(self, 'ai_chat_notes', None),
                'members': [],
                'created_at': None,
                'submitter': getattr(self, 'submitter', ''),
                'access_code': getattr(self, 'access_code', '')
            }


class TeamMember(db.Model):
    """团队成员表"""
    __tablename__ = 'team_members'
    
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    team_id = db.Column(db.BigInteger, db.ForeignKey('teams.id', ondelete='CASCADE'), nullable=False, index=True)
    name = db.Column(db.String(50), nullable=False)
    is_leader = db.Column(db.Boolean, default=False, nullable=False)
    
    def to_dict(self):
        return {
            'name': self.name,
            'is_leader': self.is_leader
        }


class Conversation(db.Model):
    """对话记录表"""
    __tablename__ = 'conversations'
    
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    team_id = db.Column(db.BigInteger, db.ForeignKey('teams.id', ondelete='CASCADE'), nullable=False, index=True)
    conversation_id = db.Column(db.String(100), nullable=False, index=True)  # Coze API返回的会话ID
    conversation_name = db.Column(db.String(200), default='未命名会话')
    session_name = db.Column(db.String(100), index=True)  # 团队访问码
    created_at = db.Column(db.DateTime, default=datetime.now, nullable=False)
    saved_at = db.Column(db.DateTime, default=datetime.now, nullable=False)
    
    # 关联关系
    messages = db.relationship('ConversationMessage', backref='conversation', lazy=True, cascade='all, delete-orphan')
    
    def to_dict(self):
        return {
            'conversation_id': self.conversation_id,
            'conversation_name': self.conversation_name,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'message_count': len(self.messages),
            'messages': [m.to_dict() for m in self.messages]
        }


class ConversationMessage(db.Model):
    """对话消息表"""
    __tablename__ = 'conversation_messages'
    
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    conversation_id = db.Column(db.Integer, db.ForeignKey('conversations.id', ondelete='CASCADE'), nullable=False, index=True)
    role = db.Column(db.String(20), nullable=False)  # 'user' 或 'assistant'
    content = db.Column(db.Text, nullable=False)
    message_id = db.Column(db.String(100))  # Coze API返回的消息ID
    created_at = db.Column(db.DateTime)
    
    def to_dict(self):
        return {
            'role': self.role,
            'content': self.content,
            'text': self.content,  # 兼容字段
            'message_id': self.message_id,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }




