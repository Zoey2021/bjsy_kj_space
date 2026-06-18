-- 校本课程「科创启航」封面与册次名称（已有库执行）
USE learn_space;

UPDATE course_grade
SET name = '二年级上册',
    description = '科创启航 · 杭州市滨江实验小学（上册，内容建设中）',
    cover_url = NULL,
    sort_order = 90
WHERE textbook_type = 'SCHOOL' AND sort_order = 90;

UPDATE course_grade
SET name = '二年级下册',
    description = '科创启航 · 杭州市滨江实验小学（二年级下册）',
    cover_url = '/course-covers/school-innovation-down.png',
    sort_order = 91
WHERE textbook_type = 'SCHOOL' AND sort_order = 91;

-- 若 sort_order 已对不上，按旧占位名称更新
UPDATE course_grade
SET name = '二年级上册',
    description = '科创启航 · 杭州市滨江实验小学（上册，内容建设中）',
    cover_url = NULL,
    sort_order = 90
WHERE textbook_type = 'SCHOOL' AND name IN ('校本主题学习（一）', '二年级上册');

UPDATE course_grade
SET name = '二年级下册',
    description = '科创启航 · 杭州市滨江实验小学（二年级下册）',
    cover_url = '/course-covers/school-innovation-down.png',
    sort_order = 91
WHERE textbook_type = 'SCHOOL' AND name IN ('校本主题学习（二）', '二年级下册');
