-- 校本课程：新增「人工智能启蒙」上册、下册
SET NAMES utf8mb4;
USE learn_space;

INSERT INTO course_grade (name, sort_order, description, textbook_type, cover_url, pdf_url)
SELECT '上册', 94, '人工智能启蒙 · 人工智能启蒙（上册）', 'SCHOOL', '/course-covers/ai-enlighten-up.png', NULL
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM course_grade WHERE textbook_type = 'SCHOOL' AND sort_order = 94
);

INSERT INTO course_grade (name, sort_order, description, textbook_type, cover_url, pdf_url)
SELECT '下册', 95, '人工智能启蒙 · 人工智能启蒙（下册）', 'SCHOOL', '/course-covers/ai-enlighten-down.png', NULL
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM course_grade WHERE textbook_type = 'SCHOOL' AND sort_order = 95
);
