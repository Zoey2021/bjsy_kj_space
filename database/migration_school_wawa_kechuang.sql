-- 校本课程：更新「科创启航」封面，新增「娃娃讲科技」上下册
-- 执行示例：mysql --default-character-set=utf8mb4 -u... learn_space < migration_school_wawa_kechuang.sql
SET NAMES utf8mb4;
USE learn_space;

UPDATE course_grade
SET description = '科创启航 · 洞察未来科创启航（二年级上册）',
    cover_url = '/course-covers/kechuang-up.png'
WHERE textbook_type = 'SCHOOL' AND sort_order = 90;

UPDATE course_grade
SET description = '科创启航 · 洞察未来科创启航（二年级下册）',
    cover_url = '/course-covers/kechuang-down.png'
WHERE textbook_type = 'SCHOOL' AND sort_order = 91;

INSERT INTO course_grade (name, sort_order, description, textbook_type, cover_url, pdf_url)
SELECT '二年级上册', 92, '娃娃讲科技 · 少儿趣味科普课堂（二年级上册）', 'SCHOOL', '/course-covers/wawa-tech-up.png', NULL
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM course_grade WHERE textbook_type = 'SCHOOL' AND sort_order = 92
);

INSERT INTO course_grade (name, sort_order, description, textbook_type, cover_url, pdf_url)
SELECT '二年级下册', 93, '娃娃讲科技 · 少儿趣味科普课堂（二年级下册）', 'SCHOOL', '/course-covers/wawa-tech-down.png', NULL
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM course_grade WHERE textbook_type = 'SCHOOL' AND sort_order = 93
);
