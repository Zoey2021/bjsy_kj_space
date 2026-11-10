# 信息科技学习空间

面向中小学信息科技课堂的数字化学习平台：学生按课次完成探究活动，教师查看学情、布置支架与评价，学校侧可管理班级与账号。

线上环境示例：[bjsysmartedu.cn](https://bjsysmartedu.cn)

---

## 功能概览

| 角色 | 主要能力 |
|------|----------|
| **学生** | 课程地图、课次工作台（探究 / 小测 / 学习评价）、积分与兑换商城、游学乐园、AI 实验室（小派老师等） |
| **教师** | 班级学情大屏、评价中心、分层脚手架、备课与课次预览、班级 / 学生管理、钉钉登录（可选） |
| **学校管理** | 教师 / 班级 / 学生账号、校本课程入口等 |

当前教材重心：

- **四年级上册**：数据主题探究课（含第 7 课《数据计算》等）
- **六年级上册**：算法实现单元（第 5～7 课互动页；第 1～4 课资源已收回，学生端显示「已过期」）

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、Vite、Element Plus、Vue Router、Axios、Chart.js |
| 后端 | Java 8、Spring Boot 2.7、JWT、MyBatis / JdbcTemplate |
| 数据 | MySQL 8 |
| 部署 | Docker Compose、Nginx 反代；大体积教材 / 校本课件建议走 **OSS** |

---

## 目录结构

```text
├── frontend/          # 学生端 / 教师端 SPA + public 静态课页
├── backend/           # Spring Boot API
├── database/          # init.sql、课次迁移与运维说明
├── docker-compose.yml # 一键编排 mysql / backend / frontend
├── scripts/           # 服务器运维脚本（清盘、课次归档等）
├── docs/              # 补充文档
└── _archive/          # 已收回课次的本地备份（可选恢复）
```

互动课页一般在：

`frontend/public/lessons/<课次目录>/index.html`

例如：`g4-up-lesson7`、`g6-up-lesson6`、`g6-up-lesson7`。

---

## 快速开始（Docker）

### 环境要求

- Docker / Docker Compose
- 可选：配置 `.env`（可参考 `.env.example`）

### 启动

```bash
# 在项目根目录
cp -n .env.example .env   # 按需填写 AI / 钉钉等密钥
docker compose up -d --build
```

默认本机端口（仅绑定 `127.0.0.1`，不建议对公网直接开放）：

| 服务 | 地址 |
|------|------|
| 前端 | http://127.0.0.1:8088 |
| 后端 | http://127.0.0.1:8080 |
| MySQL | 127.0.0.1:3307 |

首次启动会执行 `database/init.sql` 初始化库表与课程大纲。课次互动工作台多由后端 **Seeder** 在启动时绑定 / 更新。

### 生产注意

1. **务必修改** `docker-compose.yml` / 数据库中的默认口令；弱口令账号登录后会强制改密。
2. 安全组只放行 **80 / 443**，由 Nginx 反代到本机 `8088`；勿对公网放行 `8080` / `3307`。
3. 口令加固说明见 `database/HARDEN_README.md`（含明文临时口令的 SQL **不进仓库**）。
4. 大课件目录 `frontend/public/school/` 默认不进 Git，线上由 Nginx 跳转 OSS。

---

## 本地开发（可选）

**前端**

```bash
cd frontend
npm install
npm run dev
```

**后端**

```bash
cd backend
mvn spring-boot:run
```

需自行准备 MySQL，并配置对应 `application-*.yml` / 环境变量。

---

## 常见运维

| 场景 | 说明 |
|------|------|
| 评价中心未生效 | 见 `EVAL_CENTER_DEPLOY.md` / `fix-eval-center-on-server.sh` |
| 六年级第 7 课绑定 | `database/bind_g6_up_lesson7.sql`、`fix-g6-lesson7-on-server.sh` |
| 四年级第 7 课 | `database/migration_g4_up_lesson7.sql`、`fix-g4-lesson7-on-server.sh` |
| 磁盘紧张 / 收回前 4 课 | `database/HARDEN_DISK_G6_1_4.md`、`scripts/free-disk-archive-g6-1-4.sh` |

部署包 zip 默认被 `.gitignore` 忽略，请勿把密钥、`.env`、整库 SQL 推到公开仓库。

---

## 贡献与协作

1. 在功能分支开发，保持提交说明简洁（本仓库常用「保存当前版本：…」风格）。
2. 课次 HTML 改动后注意前端缓存参数 / 挂载卷，避免学生端仍读到旧页。
3. 涉及账号口令的脚本仅在服务器本地使用，不要提交明文密码。

---

## 许可与归属

本项目用于杭州市滨江实验小学等信息科技教学场景的校本建设与课堂实践。  
如需二次开发或对外发布，请先与项目维护者确认使用范围。
