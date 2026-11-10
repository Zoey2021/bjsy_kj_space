# 账号口令与端口加固

针对安全报告中「弱口令」与公网直连 `8088` 的风险，已做如下改动。

## 1. 立刻在生产库执行 SQL

`database/harden_passwords.sql` **含临时明文口令，不进 Git**。请在服务器本地保留该文件后执行：

```bash
docker exec -i learn-mysql mysql -ulearnuser -p'<数据库密码>' learn_space < /www/wwwroot/learn-space/database/harden_passwords.sql
```

或在 phpMyAdmin 导入该 SQL。

- 会轮换 `admin` / `teacher1` 弱口令，并禁用公开演示学生账号
- **登录后请马上把 admin 改成自己的强口令**

## 2. 部署本仓库后端 + 前端

应用层改动：

- 仍为弱口令的账号登录后强制改密，未改密前不能调业务 API
- 教师/管理员新密码至少 8 位且含字母+数字
- 同一账号短时间密码错误过多会暂时锁定
- Docker 端口改为仅 `127.0.0.1` 绑定（`8088`/`8080`/`3307`）

建议宝塔安全组/防火墙：**不要**对公网放行 8088、8080、3307；只放行 80/443，由 Nginx 反代到 `127.0.0.1:8088`。

## 3. 不要再执行旧的弱口令恢复脚本

`database/restore_teacher_admin.sql` 可能把密码写回弱口令，生产环境请勿使用。需要恢复管理员时用服务器本地的 `harden_passwords.sql` 或后台「重置密码」。
