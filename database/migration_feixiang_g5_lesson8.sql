-- 五年级下册 第8课：绑定飞象探究单 HTML + 提交任务（已有库执行一次即可）
USE learn_space;

-- 删除本课旧资源/任务（若重复执行）
DELETE ct FROM course_task ct
INNER JOIN course_lesson cl ON ct.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '五年级下册' AND cl.title = '第8课 体验控制系统';

DELETE cr FROM course_resource cr
INNER JOIN course_lesson cl ON cr.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '五年级下册' AND cl.title = '第8课 体验控制系统';

INSERT INTO course_resource (lesson_id, title, res_type, content_url, content_text, sort_order)
SELECT cl.id, '体验控制系统探究单', 'WEB', '/lessons/g5-lesson8/index.html', NULL, 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '五年级下册' AND cu.name = '第二单元 控制系统' AND cl.title = '第8课 体验控制系统'
LIMIT 1;

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order)
SELECT cl.id, '探究单互动挑战', '飞象老师探究单：体验控制系统', 'EXTERNAL', '{"source":"feixiang","path":"/lessons/g5-lesson8/index.html"}', 100, 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '五年级下册' AND cu.name = '第二单元 控制系统' AND cl.title = '第8课 体验控制系统'
LIMIT 1;
