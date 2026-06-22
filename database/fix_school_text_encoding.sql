-- 修复校本课程中文乱码（须以 utf8mb4 连接执行）
SET NAMES utf8mb4;

UPDATE course_grade
SET name = '二年级上册',
    description = '科创启航 · 洞察未来科创启航（二年级上册）'
WHERE textbook_type = 'SCHOOL' AND sort_order = 90;

UPDATE course_grade
SET name = '二年级下册',
    description = '科创启航 · 洞察未来科创启航（二年级下册）'
WHERE textbook_type = 'SCHOOL' AND sort_order = 91;

UPDATE course_grade
SET name = '二年级上册',
    description = '娃娃讲科技 · 少儿趣味科普课堂（二年级上册）'
WHERE textbook_type = 'SCHOOL' AND sort_order = 92;

UPDATE course_grade
SET name = '二年级下册',
    description = '娃娃讲科技 · 少儿趣味科普课堂（二年级下册）'
WHERE textbook_type = 'SCHOOL' AND sort_order = 93;
