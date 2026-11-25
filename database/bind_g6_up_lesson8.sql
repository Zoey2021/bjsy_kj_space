-- 六年级上第8课：绑定互动工作台（不依赖后端 Seeder）
SET @lesson_id := (
  SELECT cl.id FROM course_lesson cl
  INNER JOIN course_unit cu ON cl.unit_id = cu.id
  INNER JOIN course_grade cg ON cu.grade_id = cg.id
  WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN'
    AND cu.name='第二单元 算法的效率'
    AND cl.title='第8课 算法的多样性'
  LIMIT 1
);

SELECT IF(@lesson_id IS NULL, '未找到第8课', CONCAT('lesson_id=', @lesson_id)) AS info;

UPDATE course_lesson
SET content = CONCAT(
  '<p>本课学习<strong>算法的多样性</strong>：用折半涂色理解二分查找，补全顺序查找与二分查找流程图，',
  '并排运行两种算法比较次数，反思为什么算法需要多样性。</p>',
  '<p>请依次完成<strong>探究一～探究三</strong>，然后作答<strong>课堂小测</strong>并填写<strong>学习评价</strong>。</p>'
)
WHERE id=@lesson_id
  AND (content IS NULL OR content LIKE '%请按教师课堂安排完成学习%' OR content NOT LIKE '%算法的多样性%');

INSERT INTO course_resource (lesson_id, title, res_type, content_url, sort_order)
SELECT @lesson_id, '算法多样性探究活动', 'WEB', '/lessons/g6-up-lesson8/index.html', 1
FROM DUAL
WHERE @lesson_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM course_resource r
    WHERE r.lesson_id=@lesson_id AND r.res_type='WEB' AND r.content_url='/lessons/g6-up-lesson8/index.html'
  );

-- 工作台 JSON 请以后端 seed/g6-up-lesson8-workspace.json 为准；此处仅保证 EXTERNAL 任务存在时由 Seeder 覆盖。
SELECT cl.id, cl.title,
       (SELECT COUNT(*) FROM course_resource r WHERE r.lesson_id=cl.id AND r.content_url LIKE '%g6-up-lesson8%') web_cnt
FROM course_lesson cl WHERE cl.id=@lesson_id;
