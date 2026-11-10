# 六年级上第1–4课收回 & 腾磁盘

## 学生端效果
- 六年级上册第1–4课在目录中显示「已过期」（不论是否已完成）
- 点击或直达链接只提示资源已收回，不再打开互动页
- 教师预览不受影响

## 磁盘说明（重要）
本仓库内六年级上第1–4课互动资源合计约 **1.6MB**，腾出量很小。  
ECS 上更常见的大头是：

| 路径 | 约占用 | 说明 |
|------|--------|------|
| `frontend/public/school/kechuang-up` | 数 GB | nginx 已 301 到 OSS，**本机可删** |
| Docker 镜像/构建缓存 | 不定 | `docker builder prune` |
| MySQL 数据卷 | 勿删 | 学习记录在库里，与静态页无关 |

## 服务器操作
1. 上传并解压部署包（含前端「已过期」逻辑 + 占位 HTML）
2. 执行：

```bash
cd /www/wwwroot/learn-space
bash scripts/free-disk-archive-g6-1-4.sh
# 按提示确认是否删除 school 本机副本、是否清理 Docker 缓存
docker compose up -d --build frontend
```

3. 原互动页备份在服务器 `_archive/g6-up-lessons-1-4-YYYYMMDD/`（若执行前仍是完整页）
4. 本地仓库备份：`_archive/g6-up-lessons-1-4-20261110/`

## 恢复某课
把 `_archive/.../g6-up-lessonN` 拷回 `frontend/public/lessons/`，并从 `frontend/src/constants/expiredLessons.js` 去掉对应规则后重建前端。
