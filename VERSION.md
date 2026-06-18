# 稳定版本快照 · 2026-06-18

本文件标记当前已验收的功能基线，便于后续开发时对照回退。

## 已验收功能

### 登录
- 左右分栏登录页（封面图 + 信息组 logo）
- 学生/教师切换，班级码登录（6 位码 → 选姓名）
- 选名弹窗：8 列 × 6 行，半透明毛玻璃样式

### 班级码
- 教师「班级学情」：选班后生成 6 位码（8 小时有效），可复制/刷新
- 学生凭班级码选名登录，进入课程地图

### 学生课时工作台
- 左侧：课程介绍 / 学习记录 / 积分榜 + 探究一/二/三菜单
- 右侧：仅展示当前探究内容（iframe 嵌入模式，无顶部 Tab 切换）
- 活动命名：**探究一：绘制流程图** 等
- 进入课时默认显示「课程介绍」

### 教师班级学情
- 三 Tab 雷达图（小测：知识掌握/能力提升/素养变化；评价：学习兴趣/学习难度/学习信心）
- 班级码黄色横幅展示

### 积分
- 每完成一个探究活动（检查通过/验证成功）**+2 积分**，同一活动仅奖励一次
- 签到 +2 积分（前端本地）

## 关键文件

| 功能 | 路径 |
|------|------|
| 登录页 | `frontend/src/views/Login.vue` |
| 学生课时 | `frontend/src/views/student/LessonDetail.vue` |
| 探究互动页 | `frontend/public/lessons/g6-up-lesson1/index.html` |
| 班级学情 | `frontend/src/views/teacher/ClassLearningDashboard.vue` |
| 积分逻辑 | `backend/.../LearnService.java` |

## 部署

```bash
docker compose build frontend && docker compose up -d frontend
# 后端若需更新积分文案等：
docker compose build backend && docker compose up -d backend
```

访问：http://localhost （强制刷新 Cmd+Shift+R）
