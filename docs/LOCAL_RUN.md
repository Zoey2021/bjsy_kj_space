# 本地运行步骤（新手分步教程）

## 环境准备

请提前安装以下软件：

| 软件 | 版本要求 | 下载 |
|------|----------|------|
| JDK | 8 | https://adoptium.net/ |
| Maven | 3.6+ | https://maven.apache.org/ |
| Node.js | 18+ | https://nodejs.org/ |
| MySQL | 8.0 | https://dev.mysql.com/downloads/ |
| Docker（可选） | 最新 | https://www.docker.com/ |

验证安装：
```bash
java -version    # 应显示 1.8.x
mvn -version
node -v
npm -v
mysql --version
```

---

## 方式一：本地分别启动（推荐开发调试）

### 第 1 步：创建并导入数据库

1. 启动 MySQL 服务
2. 执行建库脚本：

```bash
mysql -u root -p < database/init.sql
```

或在 MySQL 客户端中打开 `database/init.sql` 执行。

### 第 2 步：修改后端数据库配置

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/learn_space?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root          # 【改成你的MySQL用户名】
    password: your_password # 【改成你的MySQL密码】
```

### 第 3 步：启动后端

```bash
cd backend
mvn spring-boot:run
```

看到 `Started LearnSpaceApplication` 表示成功，后端地址：`http://localhost:8080`

### 第 4 步：启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器打开：`http://localhost:5173`

### 第 5 步：测试登录

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | admin | 123456 |
| 教师 | teacher1 | 123456 |
| 学生 | student1 | 123456 |

### 第 6 步：测试 SSE 看板

1. 用 `teacher1` 登录，进入「班级学情看板」，选择「七年级1班」
2. 另开浏览器窗口，用 `student1` 登录，完成课时任务并提交
3. 教师看板应**自动更新**提交人数（无需刷新）

### 第 7 步：接口测试（可选）

```bash
# 登录获取 Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student1","password":"123456"}'

# 用返回的 token 访问课程地图
curl http://localhost:8080/api/course/map \
  -H "Authorization: Bearer 你的token"
```

---

## 方式二：Docker 一键启动

### 第 1 步：确认已安装 Docker 和 Docker Compose

```bash
docker -v
docker compose version
```

### 第 2 步：修改配置（如需要）

- `docker-compose.yml` 中 MySQL 密码
- `backend/src/main/resources/application-docker.yml` 中数据库连接（一般不用改）

### 第 3 步：一键构建并启动

```bash
cd 信息科技学习空间
docker compose up -d --build
```

### 第 4 步：访问

浏览器打开：`http://localhost` （Nginx 80 端口）

---

## 项目目录说明

```
信息科技学习空间/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue3 前端
├── database/         # MySQL 建库脚本
├── docker/           # Nginx 配置
├── docs/             # 文档
└── docker-compose.yml
```
