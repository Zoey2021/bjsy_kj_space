-- 六年级上第5课：去掉探究副标题（Bug猎人 / 完善算法 / 通用化改造）
USE learn_space;

UPDATE course_task ct
INNER JOIN course_lesson cl ON ct.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET
  ct.config_json = REPLACE(
    REPLACE(
      REPLACE(
        REPLACE(
          REPLACE(
            REPLACE(ct.config_json,
              '"title":"Bug猎人"', '"title":""'),
            '"title": "Bug猎人"', '"title":""'
          ),
          '"title":"完善算法"', '"title":""'
        ),
        '"title": "完善算法"', '"title":""'
      ),
      '"title":"通用化改造"', '"title":""'
    ),
    '"title": "通用化改造"', '"title":""'
  ),
  ct.description = '六年级上第5课学生工作台'
WHERE cg.name = '六年级上册'
  AND cg.textbook_type = 'MAIN'
  AND cu.name LIKE '%算法的实现%'
  AND (cl.title LIKE '%第5课%' OR cl.title LIKE '%算法执行%' OR cl.title LIKE '%算法的执行%')
  AND ct.task_type = 'EXTERNAL';

-- 核对
SELECT cl.title, JSON_EXTRACT(ct.config_json, '$.activities[*].title') AS activity_titles
FROM course_task ct
INNER JOIN course_lesson cl ON ct.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册'
  AND cl.title LIKE '%第5课%';
