-- 六年级上册 2026 版目录更新（18 课）
-- 保留已有课时 ID 与探究活动绑定，仅更新标题并补充第三单元新课

UPDATE course_grade
SET description = '义务教育教科书·信息科技（六年级上册·2026版）'
WHERE name = '六年级上册' AND textbook_type = 'MAIN';

-- 第一单元：重命名第 4～7 课
UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.title = '第4课 用程序设计语言描述算法',
    cl.content = '<p>本课属于《六年级上册》（2026版）《第一单元 算法的实现》。</p><p>请按教师课堂安排完成学习。</p>'
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现'
  AND cl.title = '第4课 算法的程序体验';

UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.title = '第5课 算法执行',
    cl.content = '<p>本课属于《六年级上册》（2026版）《第一单元 算法的实现》。</p><p>请按教师课堂安排完成学习。</p>'
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现'
  AND cl.title = '第5课 算法的执行';

UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.title = '第6课 “猜数字”算法设计',
    cl.content = '<p>本课属于《六年级上册》（2026版）《第一单元 算法的实现》。</p><p>请按教师课堂安排完成学习。</p>'
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现'
  AND cl.title = '第6课 猜数字算法设计';

UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.title = '第7课 用人工智能辅助算法实现',
    cl.content = '<p>本课属于《六年级上册》（2026版）《第一单元 算法的实现》。</p><p>请按教师课堂安排完成学习。</p>'
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现'
  AND cl.title = '第7课 猜数字算法验证';

-- 第二单元：重命名第 12 课
UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.title = '第12课 用人工智能解决“韩信点兵”问题',
    cl.content = '<p>本课属于《六年级上册》（2026版）《第二单元 算法的效率》。</p><p>请按教师课堂安排完成学习。</p>'
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第二单元 算法的效率'
  AND cl.title = '第12课 “韩信点兵”同余法的实现';

-- 第三单元：先顺延原第 14、15 课，再插入新课
UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.title = '第16课 算法对生活的影响',
    cl.sort_order = 4,
    cl.content = '<p>本课属于《六年级上册》（2026版）《第三单元 算法的影响》。</p><p>请按教师课堂安排完成学习。</p>'
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第三单元 算法的影响'
  AND cl.title = '第14课 算法对生活的影响';

UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.title = '第17课 人机对话的实现',
    cl.sort_order = 5,
    cl.content = '<p>本课属于《六年级上册》（2026版）《第三单元 算法的影响》。</p><p>请按教师课堂安排完成学习。</p>'
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第三单元 算法的影响'
  AND cl.title = '第15课 人机对话的实现';

UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.content = '<p>本课属于《六年级上册》（2026版）《第三单元 算法的影响》。</p><p>请按教师课堂安排完成学习。</p>'
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第三单元 算法的影响'
  AND cl.title = '第13课 在线生活中的算法';

INSERT INTO course_lesson (unit_id, title, sort_order, content, duration_min)
SELECT cu.id, '第14课 智能寻路中的搜索算法', 2,
  '<p>本课属于《六年级上册》（2026版）《第三单元 算法的影响》。</p><p>请按教师课堂安排完成学习。</p>', 40
FROM course_unit cu
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第三单元 算法的影响'
  AND NOT EXISTS (
    SELECT 1 FROM course_lesson cl2
    WHERE cl2.unit_id = cu.id AND cl2.title = '第14课 智能寻路中的搜索算法'
  )
LIMIT 1;

INSERT INTO course_lesson (unit_id, title, sort_order, content, duration_min)
SELECT cu.id, '第15课 在线购物中的推荐算法', 3,
  '<p>本课属于《六年级上册》（2026版）《第三单元 算法的影响》。</p><p>请按教师课堂安排完成学习。</p>', 40
FROM course_unit cu
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第三单元 算法的影响'
  AND NOT EXISTS (
    SELECT 1 FROM course_lesson cl2
    WHERE cl2.unit_id = cu.id AND cl2.title = '第15课 在线购物中的推荐算法'
  )
LIMIT 1;

INSERT INTO course_lesson (unit_id, title, sort_order, content, duration_min)
SELECT cu.id, '第18课 对话机器人', 6,
  '<p>本课属于《六年级上册》（2026版）《第三单元 算法的影响》。</p><p>请按教师课堂安排完成学习。</p>', 40
FROM course_unit cu
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第三单元 算法的影响'
  AND NOT EXISTS (
    SELECT 1 FROM course_lesson cl2
    WHERE cl2.unit_id = cu.id AND cl2.title = '第18课 对话机器人'
  )
LIMIT 1;

-- 同步第 4、5 课工作台标题（保留探究活动路径）
UPDATE course_task ct
INNER JOIN course_lesson cl ON ct.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET ct.title = '用程序设计语言描述算法探究',
    ct.description = '六年级上第4课学生工作台',
    ct.config_json = JSON_SET(ct.config_json, '$.lessonTitle', '第4课 用程序设计语言描述算法')
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现'
  AND cl.title = '第4课 用程序设计语言描述算法'
  AND ct.task_type = 'EXTERNAL';

UPDATE course_resource cr
INNER JOIN course_lesson cl ON cr.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cr.title = '用程序设计语言描述算法探究活动'
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现'
  AND cl.title = '第4课 用程序设计语言描述算法'
  AND cr.res_type = 'WEB';

UPDATE course_task ct
INNER JOIN course_lesson cl ON ct.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET ct.title = '算法执行探究',
    ct.description = '六年级上第5课学生工作台',
    ct.config_json = JSON_SET(ct.config_json, '$.lessonTitle', '第5课 算法执行')
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现'
  AND cl.title = '第5课 算法执行'
  AND ct.task_type = 'EXTERNAL';

UPDATE course_resource cr
INNER JOIN course_lesson cl ON cr.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cr.title = '算法执行探究活动'
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现'
  AND cl.title = '第5课 算法执行'
  AND cr.res_type = 'WEB';
