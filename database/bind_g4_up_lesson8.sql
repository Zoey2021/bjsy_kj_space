-- 四年级上册 第8课 图表呈现：绑定上交作品页 + EXTERNAL 工作台
UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.content = CONCAT(
  '<p>本课学习<strong>图表呈现</strong>：把表格数据做成合适的图表，写清标题和单位，并把作品上传到学习空间。</p>',
  '<p>请从左侧完成<strong>上交作品</strong>，然后作答<strong>课堂小测</strong>并填写<strong>学习评价</strong>。</p>'
)
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第二单元 数据证明观点' AND cl.title = '第8课 图表呈现';

INSERT INTO course_resource (lesson_id, title, res_type, content_url, sort_order)
SELECT cl.id, '图表呈现 · 上交作品', 'WEB', '/lessons/g4-up-lesson8/index.html', 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第二单元 数据证明观点' AND cl.title = '第8课 图表呈现'
  AND NOT EXISTS (
    SELECT 1 FROM course_resource r
    WHERE r.lesson_id = cl.id AND r.res_type = 'WEB'
      AND r.content_url = '/lessons/g4-up-lesson8/index.html'
  );

-- config_json 由应用 Seeder 或部署脚本写入完整 workspace；此处仅保证有 EXTERNAL 任务壳
INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order)
SELECT cl.id, '图表呈现 · 上交作品',
  '四年级上第8课学生工作台：上交作品、学习评价、课堂小测',
  'EXTERNAL', '{"layout":"student_workspace","pending":"seed"}', 100, 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第二单元 数据证明观点' AND cl.title = '第8课 图表呈现'
  AND NOT EXISTS (
    SELECT 1 FROM course_task t
    WHERE t.lesson_id = cl.id AND t.task_type = 'EXTERNAL'
  );
