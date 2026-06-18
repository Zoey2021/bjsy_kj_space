-- 自查：配套教材是否有重复单元、课时是否串课
USE learn_space;

-- 1) 同一册下重复的单元名（有重复则需先跑 migration_course_outline.sql 全量脚本）
SELECT cg.name AS 册次, cu.name AS 单元, COUNT(*) AS 条数, GROUP_CONCAT(cu.id ORDER BY cu.id) AS 单元id列表
FROM course_unit cu
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.textbook_type = 'MAIN'
GROUP BY cg.id, cu.name
HAVING COUNT(*) > 1;

-- 2) 五年级下册 · 第一单元 应只有 3 课（第1~3课）
SELECT cu.id AS 单元id, cl.sort_order, cl.title AS 课时
FROM course_lesson cl
JOIN course_unit cu ON cl.unit_id = cu.id
JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '五年级下册' AND cu.name = '第一单元 生活中的系统'
ORDER BY cl.sort_order;
