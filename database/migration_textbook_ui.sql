-- ============================================================
-- 已有库升级到「配套教材 + 封面」结构（执行前请备份 learn_space）
-- 1) 增加字段  2) 清空课程与学生学习记录后重新灌入三年级～六年级配套 + 校本占位
-- 若 textbook_type 列已存在，跳过 ALTER 或注释掉对应行
-- ============================================================

USE learn_space;

-- 若尚未执行 migration_add_textbook_columns.sql，请先执行；下列语句会清空课程与学生学习记录
DELETE FROM learn_submission;
DELETE FROM learn_progress;
DELETE FROM learn_visit_log;
DELETE FROM learn_points;
DELETE FROM course_task;
DELETE FROM course_resource;
DELETE FROM course_lesson;
DELETE FROM course_unit;
DELETE FROM course_grade;

INSERT INTO course_grade (name, sort_order, description, textbook_type, cover_url) VALUES
('三年级上册', 1, '义务教育教科书·信息科技（三年级上册）', 'MAIN', '/course-covers/grade3-up.jpg'),
('三年级下册', 2, '义务教育教科书·信息科技（三年级下册）', 'MAIN', '/course-covers/grade3-down.jpg'),
('四年级上册', 3, '义务教育教科书·信息科技（四年级上册）', 'MAIN', '/course-covers/grade4-up.jpg'),
('四年级下册', 4, '义务教育教科书·信息科技（四年级下册）', 'MAIN', '/course-covers/grade4-down.jpg'),
('五年级上册', 5, '义务教育教科书·信息科技（五年级上册）', 'MAIN', '/course-covers/grade5-up.jpg'),
('五年级下册', 6, '义务教育教科书·信息科技（五年级下册）', 'MAIN', '/course-covers/grade5-down.jpg'),
('六年级上册', 7, '义务教育教科书·信息科技（六年级上册）', 'MAIN', '/course-covers/grade6-up.jpg'),
('六年级下册', 8, '义务教育教科书·信息科技（六年级下册）', 'MAIN', '/course-covers/grade6-down.jpg'),
('校本主题学习（一）', 90, '本校信息科技特色内容，建设中', 'SCHOOL', NULL),
('校本主题学习（二）', 91, '本校信息科技实践活动，建设中', 'SCHOOL', NULL);

INSERT INTO course_unit (grade_id, name, sort_order, description) VALUES
(1, '第一单元 走进信息科技', 1, NULL), (2, '第一单元 走进信息科技', 1, NULL), (3, '第一单元 走进信息科技', 1, NULL), (4, '第一单元 走进信息科技', 1, NULL),
(5, '第一单元 走进信息科技', 1, NULL), (6, '第一单元 走进信息科技', 1, NULL), (7, '第一单元 走进信息科技', 1, NULL), (8, '第一单元 走进信息科技', 1, NULL);

INSERT INTO course_lesson (unit_id, title, sort_order, content, duration_min) VALUES
(1, '第1课 信息与信息技术', 1, '<p>了解信息与信息技术在生活中的体现。</p>', 40), (1, '第2课 学习规范与安全', 2, '<p>机房规范与信息安全意识。</p>', 40),
(2, '第1课 信息与信息技术', 1, '<p>了解信息与信息技术在生活中的体现。</p>', 40), (2, '第2课 学习规范与安全', 2, '<p>机房规范与信息安全意识。</p>', 40),
(3, '第1课 信息与信息技术', 1, '<p>了解信息与信息技术在生活中的体现。</p>', 40), (3, '第2课 学习规范与安全', 2, '<p>机房规范与信息安全意识。</p>', 40),
(4, '第1课 信息与信息技术', 1, '<p>了解信息与信息技术在生活中的体现。</p>', 40), (4, '第2课 学习规范与安全', 2, '<p>机房规范与信息安全意识。</p>', 40),
(5, '第1课 信息与信息技术', 1, '<p>了解信息与信息技术在生活中的体现。</p>', 40), (5, '第2课 学习规范与安全', 2, '<p>机房规范与信息安全意识。</p>', 40),
(6, '第1课 信息与信息技术', 1, '<p>了解信息与信息技术在生活中的体现。</p>', 40), (6, '第2课 学习规范与安全', 2, '<p>机房规范与信息安全意识。</p>', 40),
(7, '第1课 信息与信息技术', 1, '<p>了解信息与信息技术在生活中的体现。</p>', 40), (7, '第2课 学习规范与安全', 2, '<p>机房规范与信息安全意识。</p>', 40),
(8, '第1课 信息与信息技术', 1, '<p>了解信息与信息技术在生活中的体现。</p>', 40), (8, '第2课 学习规范与安全', 2, '<p>机房规范与信息安全意识。</p>', 40);

INSERT INTO course_resource (lesson_id, title, res_type, content_text, sort_order) VALUES
(1, '本册导学', 'TEXT', '<h3>学习目标</h3><p>本学期我们将学习信息与信息技术基础内容。</p>', 1),
(1, '拓展阅读', 'TEXT', '<p>生活中处处有信息，尝试举出三个例子。</p>', 2);

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order) VALUES
(1, '任务：信息识别小练习', '判断下列说法', 'CHOICE', '{"questions":[{"id":1,"text":"天气预报属于信息吗？","options":["是","否"],"answer":"是"},{"id":2,"text":"噪音属于信息吗？","options":["是","否"],"answer":"否"}]}', 10, 1),
(1, '任务：学习心得', '写下你对信息的理解', 'FORM', '{"fields":[{"name":"content","label":"我的心得","type":"textarea","required":true}]}', 10, 2),
(2, '任务：获取方式', '写出一种你常用的信息获取方式', 'FORM', '{"fields":[{"name":"method","label":"获取方式","type":"text","required":true}]}', 10, 1);
