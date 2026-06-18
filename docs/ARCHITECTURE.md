# 中小学信息科技学习空间 - 整体架构说明

## 一、架构总览

本项目采用**前后端分离**的单体架构，适合单人开发与 500 人并发场景，不使用微服务/消息队列。

```
┌─────────────┐     HTTP/HTTPS      ┌─────────────┐     JDBC      ┌──────────┐
│  Vue3 前端   │ ◄────────────────► │ Spring Boot │ ◄───────────► │  MySQL   │
│ Element Plus│     REST API + SSE  │   后端 API   │               │   8.0    │
└─────────────┘                     └─────────────┘               └──────────┘
       ▲                                    ▲
       │                                    │
       └──────── Nginx 反向代理 ────────────┘
                    Docker Compose
```

## 二、各层职责

### 1. 表现层（Vue3 前端）

| 目录/模块 | 职责 |
|-----------|------|
| `views/Login.vue` | 统一登录，保存 JWT |
| `views/student/` | 学生课程地图、学习页、提交页 |
| `views/teacher/` | 教师 SSE 看板、完成矩阵 |
| `views/admin/` | 管理员账号与课程管理 |
| `views/ai/` | AI 体验中心（TTS/OCR/人脸/算法演示） |
| `api/request.js` | Axios 封装，自动携带 Token |
| `router/index.js` | 路由守卫，按角色跳转 |

### 2. 接口层（Controller）

- 接收 HTTP 请求，参数校验，调用 Service
- `AuthController`：登录发 Token
- `CourseController`：课程地图数据
- `LearnController`：学习进度、任务提交
- `DashboardController`：**SSE 实时推送**学情看板
- `AdminController`：管理功能

### 3. 业务层（Service）

- 封装业务逻辑：登录、进度更新、提交评分、积分计算
- `SseService`：管理教师端 SSE 连接，学生提交后广播更新

### 4. 数据层（Repository / JPA）

- `JpaRepository` 接口直接操作 MySQL
- 实体类与表一一对应，字段名自动映射

### 5. 安全层（Spring Security + JWT）

- 登录成功后签发 JWT，前端存 localStorage
- 每次请求带 `Authorization: Bearer <token>`
- `JwtAuthFilter` 解析 Token，注入用户身份
- 未登录返回 401，角色不符返回 403

### 6. 实时通信（SSE）

- 教师打开看板 → 建立 `EventSource` 长连接
- 学生提交任务 → 后端 `SseService.broadcast(classId)` 推送 JSON
- 看板页面监听 `message` 事件自动刷新图表

## 三、核心数据流

### 登录流程
```
用户输入账号密码 → POST /api/auth/login → 校验密码 → 生成JWT → 返回用户信息+角色
→ 前端存Token → 路由跳转到对应首页
```

### 学习提交流程
```
学生完成任务 → POST /api/learn/submit → 保存提交记录 → 更新进度 → 加积分
→ SseService 向该班级所有教师连接推送 → 看板自动更新
```

## 四、部署架构

```
阿里云服务器
├── Nginx :80  （静态资源 + API反向代理 + SSE）
├── frontend容器 （构建后的 dist）
├── backend容器  :8080
└── mysql容器    :3306
```

## 五、性能说明（500 并发）

- 单体 Spring Boot + 连接池（HikariCP）足够支撑 500 在线用户
- SSE 连接按班级隔离，一般每班 1 个教师连接，压力很小
- 静态资源由 Nginx 直接服务，减轻后端负担
- 数据库索引已建在常用查询字段（student_id、lesson_id、class_id）
