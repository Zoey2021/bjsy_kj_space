# 常见问题排错指南

## 1. 跨域问题（CORS）

**现象**：浏览器控制台报 `Access-Control-Allow-Origin` 错误。

**原因**：前端 `localhost:5173` 访问后端 `localhost:8080` 跨域。

**解决**：
- 开发环境：`vite.config.js` 已配置代理，`/api` 会转发到 `8080`，请用 `npm run dev` 启动，不要直接打开 dist
- 生产环境：Nginx 统一域名，不存在跨域
- 后端 `WebConfig.java` 也已配置 CORS，双重保障

---

## 2. 接口 401 未登录

**现象**：请求返回 401，页面跳回登录。

**排查**：
1. 检查 localStorage 是否有 `token`
2. 请求头是否带 `Authorization: Bearer xxx`
3. Token 是否过期（默认 24 小时），重新登录
4. 后端 `jwt.secret` 是否被修改（修改后旧 Token 失效）

---

## 3. SSE 不推送 / 看板不更新

**现象**：学生提交后，教师看板数字不变。

**排查**：
1. 教师看板页是否已连接 SSE（F12 → Network → 看到 `sse?classId=1` 且状态 pending）
2. Nginx 是否关闭了缓冲：配置中需有 `proxy_buffering off;` 和 `X-Accel-Buffering: no`
3. 学生是否属于该班级（`sys_user.class_id` 与看板选择的班级一致）
4. 后端日志是否有 `SSE broadcast` 输出
5. 本地开发时前端代理对 SSE 的支持：Vite 代理一般可用，若不行可直接连 `http://localhost:8080/api/dashboard/sse/1?token=xxx`（需带 token 参数）

---

## 4. 数据库连接失败

**现象**：后端启动报 `Communications link failure` 或 `Access denied`。

**解决**：
1. 确认 MySQL 已启动：`mysql -u root -p`
2. 确认数据库已创建：`SHOW DATABASES;` 应有 `learn_space`
3. 检查 `application.yml` 用户名密码、端口（默认 3306）
4. MySQL 8 认证插件：若报认证错误，执行：
   ```sql
   ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '你的密码';
   FLUSH PRIVILEGES;
   ```
5. Docker 环境：等 MySQL 健康检查通过后再启动后端（compose 已配置 `depends_on`）

---

## 5. 后端启动报错

### `Port 8080 already in use`
```bash
# macOS/Linux 查找并结束占用进程
lsof -i :8080
kill -9 进程ID
```

### `Table doesn't exist`
未执行 `database/init.sql`，重新导入。

### `java.lang.UnsupportedClassVersionError`
JDK 版本不对，需 JDK 8，检查 `java -version`。

### Maven 依赖下载慢
配置国内镜像，编辑 `~/.m2/settings.xml` 添加阿里云镜像。

---

## 6. 前端启动报错

### `npm install` 失败
```bash
npm cache clean --force
npm install --registry=https://registry.npmmirror.com
```

### 页面空白
F12 看 Console 报错；常见原因是 API 地址不对，检查 `vite.config.js` 代理配置。

---

## 7. Docker 部署问题

### 容器启动后无法访问
```bash
docker compose ps          # 查看容器状态
docker compose logs backend  # 查看后端日志
docker compose logs mysql    # 查看数据库日志
```

### 数据库数据丢失
`docker-compose.yml` 中 MySQL 使用 volume 持久化，不要 `docker compose down -v`（-v 会删数据卷）。

### 修改代码后未生效
```bash
docker compose up -d --build
```

---

## 8. 登录密码正确但提示错误

演示数据密码为 `123456`，BCrypt 加密存储。若手动改库，需用 BCrypt 加密：
- 在线生成 BCrypt 哈希，或
- 运行后端后调用注册接口（管理员功能）

---

## 9. 500 并发性能调优（上线前）

1. MySQL 连接池：`application.yml` 中 `maximum-pool-size: 30`
2. Nginx `worker_connections` 调大
3. JVM 内存：Dockerfile 中 `-Xms512m -Xmx1024m`
4. 开启 MySQL 慢查询日志排查慢 SQL
