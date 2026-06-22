# 稳定版本快照 · 2026-06-22

本文件标记当前已验收的功能基线，便于后续开发时对照回退。

## 已验收功能

### 登录
- 左右分栏登录页（封面图 + 信息组 logo）
- 学生/教师切换，班级码登录（6 位码 → 选姓名）
- 选名弹窗：8 列 × 6 行，半透明毛玻璃样式

### 班级码
- 教师「班级学情」：选班后生成 6 位码（8 小时有效），可复制/刷新
- 学生凭班级码选名登录，进入课程地图

### 课程地图
- **配套课程**：三至六年级上下册，封面与电子教材入口
- **校本课程**：按系列分组展示
  - **科创启航**（二年级上/下册，新封面）
  - **娃娃讲科技**（二年级上/下册，新增）
- **2021 级学生**：三至五年级配套课程卡显示「✅已完成」，点击可查看上/下册成绩

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
| 学生课程地图 | `frontend/src/views/student/CourseMap.vue` |
| 教师课程地图 | `frontend/src/views/teacher/TeacherCourseMap.vue` |
| 校本分组工具 | `frontend/src/utils/schoolTextbooks.js` |
| 学生课时 | `frontend/src/views/student/LessonDetail.vue` |
| 探究互动页 | `frontend/public/lessons/g6-up-lesson1/index.html` |
| 班级学情 | `frontend/src/views/teacher/ClassLearningDashboard.vue` |
| 积分逻辑 | `backend/.../LearnService.java` |
| 校本种子数据 | `database/init.sql` |
| 校本迁移 | `database/migration_school_wawa_kechuang.sql` |

## 部署

```bash
docker compose build frontend && docker compose up -d frontend
# 后端若需更新积分文案等：
docker compose build backend && docker compose up -d backend
# 已有数据库执行校本迁移（须 utf8mb4）：
docker exec -i learn-mysql mysql -ulearnuser -plearnpass123 --default-character-set=utf8mb4 learn_space < database/migration_school_wawa_kechuang.sql
docker exec -i learn-mysql mysql -ulearnuser -plearnpass123 --default-character-set=utf8mb4 learn_space < database/fix_school_text_encoding.sql
```

访问：http://localhost （强制刷新 Cmd+Shift+R）

---

## 版本记录

| 标签/提交 | 说明 |
|-----------|------|
| `stable-2026-06-18` / `1a5fca0` | 稳定基线：班级码、嵌入探究页、活动 +2 积分 |
| `898d4a3` | 教师导航：班级学情 ↑、活动编辑搁置（筹备中页） |
| `stable-2026-06-22` | 校本课程扩展：娃娃讲科技 + 科创启航新封面；2021 级已完成标记 |

## 教师工作台导航顺序（当前）

1. 课程地图（主推开发）
2. 班级学情
3. 活动编辑（**筹备中**，请用课程地图 → 课程管理）
4. 智能评价
5. 课堂积分
6. 后台管理
