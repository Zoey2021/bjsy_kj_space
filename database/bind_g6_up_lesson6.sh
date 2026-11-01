#!/bin/bash
# 不重建 backend：直接把六年级上册第6课 WEB + 工作台写入 MySQL
# 用法（服务器）：
#   cd /www/wwwroot/learn-space && bash database/bind_g6_up_lesson6.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JSON_FILE="$ROOT/backend/src/main/resources/seed/g6-up-lesson6-workspace.json"
HTML_FILE="$ROOT/frontend/public/lessons/g6-up-lesson6/index.html"

if [[ ! -f "$JSON_FILE" ]]; then
  echo "缺少 $JSON_FILE"
  exit 1
fi
if [[ ! -f "$HTML_FILE" ]]; then
  echo "缺少 $HTML_FILE（请先解压课次包）"
  exit 1
fi

python3 - "$JSON_FILE" <<'PY' | docker exec -i learn-mysql mysql -ulearnuser -plearnpass123 learn_space
import json, sys
path = sys.argv[1]
with open(path, "r", encoding="utf-8") as f:
    cfg = f.read().strip()
# MySQL 字符串转义
esc = cfg.replace("\\", "\\\\").replace("'", "\\'")
content = (
    "<p>本课学习<strong>猜数字算法设计</strong>：从规则中抽象变量并建模，"
    "用流程图表示分支与循环，再验证、优化算法。</p>"
    "<p>请从左侧依次完成<strong>探究一 抽象建模、探究二 画出猜数字流程图、"
    "探究三 验证与升级</strong>，然后作答<strong>课堂小测</strong>并填写<strong>学习评价</strong>。</p>"
)
content_esc = content.replace("\\", "\\\\").replace("'", "\\'")
print("""
SET NAMES utf8mb4;
SET @lesson_id := (
  SELECT cl.id FROM course_lesson cl
  INNER JOIN course_unit cu ON cl.unit_id = cu.id
  INNER JOIN course_grade cg ON cu.grade_id = cg.id
  WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
    AND cu.name = '第一单元 算法的实现'
    AND (cl.title = '第6课 “猜数字”算法设计' OR cl.title = '第6课 \"猜数字\"算法设计')
  LIMIT 1
);
SELECT IF(@lesson_id IS NULL, 'ERROR: 未找到第6课', CONCAT('lesson_id=', @lesson_id)) AS info;
UPDATE course_lesson SET content = '%s' WHERE id = @lesson_id;

INSERT INTO course_resource (lesson_id, title, res_type, content_url, sort_order)
SELECT @lesson_id, '猜数字算法设计探究活动', 'WEB', '/lessons/g6-up-lesson6/index.html', 1
FROM DUAL
WHERE @lesson_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM course_resource
    WHERE lesson_id = @lesson_id AND res_type = 'WEB'
      AND content_url = '/lessons/g6-up-lesson6/index.html'
  );

SET @task_id := (
  SELECT id FROM course_task
  WHERE lesson_id = @lesson_id AND task_type = 'EXTERNAL'
  ORDER BY sort_order ASC, id ASC LIMIT 1
);

UPDATE course_task
SET title = '猜数字算法设计探究',
    description = '六年级上第6课学生工作台：探究一抽象建模、探究二流程图、探究三验证升级、学习评价、课堂小测',
    config_json = '%s',
    max_score = 100
WHERE id = @task_id;

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order)
SELECT @lesson_id, '猜数字算法设计探究',
       '六年级上第6课学生工作台：探究一抽象建模、探究二流程图、探究三验证升级、学习评价、课堂小测',
       'EXTERNAL', '%s', 100, 1
FROM DUAL
WHERE @lesson_id IS NOT NULL AND @task_id IS NULL;

SELECT 'WEB资源' AS kind, id, content_url FROM course_resource
 WHERE lesson_id = @lesson_id AND res_type = 'WEB';
SELECT 'EXTERNAL任务' AS kind, id, title, LEFT(config_json, 40) AS cfg_head FROM course_task
 WHERE lesson_id = @lesson_id AND task_type = 'EXTERNAL';
""" % (content_esc, esc, esc))
PY

echo "绑定完成。请学生刷新课程地图后进入六年级上册第6课。"
echo "若页面 404：docker compose up -d --force-recreate frontend"
