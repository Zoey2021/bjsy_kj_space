-- 六年级上第5课：侧栏标题 → Bug修复 / 优化程序 / 通用化程序
USE learn_space;

UPDATE course_task ct
INNER JOIN course_lesson cl ON ct.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET ct.config_json = JSON_SET(
      ct.config_json,
      '$.activities[0].title', 'Bug修复',
      '$.activities[1].title', '优化程序',
      '$.activities[2].title', '通用化程序'
    ),
    ct.description = '六年级上第5课学生工作台'
WHERE cg.name = '六年级上册'
  AND cg.textbook_type = 'MAIN'
  AND (cl.title LIKE '%第5课%' OR cl.title LIKE '%算法执行%' OR cl.title LIKE '%算法的执行%')
  AND ct.task_type = 'EXTERNAL'
  AND JSON_TYPE(JSON_EXTRACT(ct.config_json, '$.activities')) = 'ARRAY';

SELECT cl.title,
       JSON_EXTRACT(ct.config_json, '$.activities[0].title') AS a1,
       JSON_EXTRACT(ct.config_json, '$.activities[1].title') AS a2,
       JSON_EXTRACT(ct.config_json, '$.activities[2].title') AS a3
FROM course_task ct
INNER JOIN course_lesson cl ON ct.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册'
  AND (cl.title LIKE '%第5课%' OR cl.title LIKE '%算法%');
