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

## 服务器操作（推荐：保常用资源清盘）

学生端已显示「已过期」后，真正能降容量的通常是 **school 本机副本 + Docker 缓存**，不是前 4 课那点 HTML。

```bash
cd /www/wwwroot/learn-space
# 交互确认（推荐先看占用再选 y）
bash scripts/free-disk-keep-active.sh

# 或全部自动确认：
# YES=1 bash scripts/free-disk-keep-active.sh
```

脚本会先验 OSS，再删本机课件副本；不动 MySQL、四年级课、六年级第 5～7 课。

旧版脚本仍可用：`bash scripts/free-disk-archive-g6-1-4.sh`

3. 原互动页备份在服务器 `_archive/g6-up-lessons-1-4-YYYYMMDD/`（若执行前仍是完整页）
4. 本地仓库备份：`_archive/g6-up-lessons-1-4-20261110/`

## 恢复某课
把 `_archive/.../g6-up-lessonN` 拷回 `frontend/public/lessons/`，并从 `frontend/src/constants/expiredLessons.js` 去掉对应规则后重建前端。
