# 智能评价上线核对

若页面仍显示「功能建设中」，说明服务器上的前端还在用旧的占位页，或镜像未按新代码重建。

## 1. 核对源码是否已覆盖

```bash
cd /www/wwwroot/learn-space
head -20 frontend/src/views/teacher/AiEvaluation.vue
```

正确时应看到 `class="eval-page"` / 「过程档案与综合等级」。  
若仍有 `ModulePlaceholder` 或「功能建设中」，请重新解压 `eval-center-*.zip` 覆盖后再继续。

```bash
ls backend/src/main/java/com/itech/learnspace/service/EvaluationService.java
ls backend/src/main/java/com/itech/learnspace/controller/EvaluationController.java
```

## 2. 强制重建（不要用旧缓存）

前端是构建进镜像的，只改源码、不 `--build` 或未清缓存会一直显示旧页：

```bash
cd /www/wwwroot/learn-space
docker compose build --no-cache frontend backend
docker compose up -d frontend backend
```

## 3. 浏览器

强制刷新（Mac：`Cmd+Shift+R`），或无无痕窗口打开：

`https://你的域名/teacher/ai-evaluation`
