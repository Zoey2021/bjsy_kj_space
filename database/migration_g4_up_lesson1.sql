-- 四年级上册第1课《身边的数据》：学生工作台（环节1场景匹配、环节2分类岛屿）
-- 可在现有库执行；后端启动时 G4UpLesson1Seeder 也会自动绑定/更新。

SET NAMES utf8mb4;

UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.content = '<p>本课学习<strong>身边的数据</strong>：从生活场景中发现数据，认识文字、数值、图像、声音、视频五类数据。</p><p>请从左侧依次完成<strong>环节1、环节2</strong>，完成后作答<strong>课堂小测</strong>。</p>'
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 泛在的数据' AND cl.title = '第1课 身边的数据'
  AND (cl.content IS NULL OR cl.content LIKE '%请按教师课堂安排完成学习%');

INSERT INTO course_resource (lesson_id, title, res_type, content_url, sort_order)
SELECT cl.id, '身边的数据探究活动', 'WEB', '/lessons/g4-up-lesson1/index.html', 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 泛在的数据' AND cl.title = '第1课 身边的数据'
  AND NOT EXISTS (
    SELECT 1 FROM course_resource r
    WHERE r.lesson_id = cl.id AND r.res_type = 'WEB' AND r.content_url = '/lessons/g4-up-lesson1/index.html'
  )
LIMIT 1;

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order)
SELECT cl.id, '身边的数据探究', '四年级上第1课学生工作台：环节1场景匹配、环节2分类岛屿', 'EXTERNAL',
  '{"layout":"student_workspace","lessonTitle":"第1课 身边的数据","objectives":"我能从身边的生活场景中发现数据，并能把文字、数值、图像、声音、视频正确分类。","introMap":{"title":"身边的数据","branches":[{"label":"3 本课学习目标","nodes":[{"text":"从生活场景中认出数据"},{"text":"区分文字、数值、图像、声音、视频"}]},{"label":"1. 核心学习内容","nodes":[{"text":"数据就在身边"},{"text":"五种常见数据类型"}]},{"label":"2. 课堂完成顺序","nodes":[{"text":"环节1：看场景，匹配数据类型"},{"text":"环节2：把数据分到对应岛屿"},{"text":"完成后作答课堂小测"}]}]},"activities":[{"index":1,"title":"场景匹配","step":1,"path":"/lessons/g4-up-lesson1/index.html","unlocked":true},{"index":2,"title":"分类岛屿","step":2,"path":"/lessons/g4-up-lesson1/index.html","unlocked":false}],"quiz":{"title":"课堂小测","unlocked":false,"unlockAfterActivity":2,"questions":[{"id":"k1","stem":"图书馆播放的故事录音，主要属于哪类数据？","options":["文字数据","声音数据","数值数据","图像数据"],"answer":1,"dimension":"知识掌握","explanation":"声音可以被录下来并播放，属于声音数据。"},{"id":"k2","stem":"电子温度计显示 28℃，这个“28”属于哪类数据？","options":["图像数据","视频数据","文字数据","数值数据"],"answer":3,"dimension":"知识掌握","explanation":"数字可以表示温度、数量、成绩等信息，属于数值数据。"},{"id":"a1","stem":"课程表中“周一 数学”这样的内容，主要属于哪类数据？","options":["文字数据","声音数据","图像数据","视频数据"],"answer":0,"dimension":"能力提升","explanation":"字、词、句子组成的内容属于文字数据。"},{"id":"a2","stem":"一张校园风景照片，主要属于哪类数据？","options":["声音数据","图像数据","数值数据","文字数据"],"answer":1,"dimension":"能力提升","explanation":"照片记录的是画面和图形，属于图像数据。"},{"id":"s1","stem":"记录植物生长变化的连续录像，主要属于哪类数据？","options":["视频数据","文字数据","声音数据","数值数据"],"answer":0,"dimension":"素养变化","explanation":"视频是连续变化的画面，常常还带有声音。"},{"id":"s2","stem":"“班级有 42 名同学”中的“42”主要属于哪类数据？","options":["图像数据","文字数据","数值数据","声音数据"],"answer":2,"dimension":"素养变化","explanation":"表示数量的数字属于数值数据。"}]}}',
  100, 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 泛在的数据' AND cl.title = '第1课 身边的数据'
  AND NOT EXISTS (
    SELECT 1 FROM course_task t WHERE t.lesson_id = cl.id AND t.task_type = 'EXTERNAL'
  )
LIMIT 1;

UPDATE course_task t
INNER JOIN course_lesson cl ON t.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET t.title = '身边的数据探究',
    t.description = '四年级上第1课学生工作台：环节1场景匹配、环节2分类岛屿',
    t.config_json = '{"layout":"student_workspace","lessonTitle":"第1课 身边的数据","objectives":"我能从身边的生活场景中发现数据，并能把文字、数值、图像、声音、视频正确分类。","introMap":{"title":"身边的数据","branches":[{"label":"3 本课学习目标","nodes":[{"text":"从生活场景中认出数据"},{"text":"区分文字、数值、图像、声音、视频"}]},{"label":"1. 核心学习内容","nodes":[{"text":"数据就在身边"},{"text":"五种常见数据类型"}]},{"label":"2. 课堂完成顺序","nodes":[{"text":"环节1：看场景，匹配数据类型"},{"text":"环节2：把数据分到对应岛屿"},{"text":"完成后作答课堂小测"}]}]},"activities":[{"index":1,"title":"场景匹配","step":1,"path":"/lessons/g4-up-lesson1/index.html","unlocked":true},{"index":2,"title":"分类岛屿","step":2,"path":"/lessons/g4-up-lesson1/index.html","unlocked":false}],"quiz":{"title":"课堂小测","unlocked":false,"unlockAfterActivity":2,"questions":[{"id":"k1","stem":"图书馆播放的故事录音，主要属于哪类数据？","options":["文字数据","声音数据","数值数据","图像数据"],"answer":1,"dimension":"知识掌握","explanation":"声音可以被录下来并播放，属于声音数据。"},{"id":"k2","stem":"电子温度计显示 28℃，这个“28”属于哪类数据？","options":["图像数据","视频数据","文字数据","数值数据"],"answer":3,"dimension":"知识掌握","explanation":"数字可以表示温度、数量、成绩等信息，属于数值数据。"},{"id":"a1","stem":"课程表中“周一 数学”这样的内容，主要属于哪类数据？","options":["文字数据","声音数据","图像数据","视频数据"],"answer":0,"dimension":"能力提升","explanation":"字、词、句子组成的内容属于文字数据。"},{"id":"a2","stem":"一张校园风景照片，主要属于哪类数据？","options":["声音数据","图像数据","数值数据","文字数据"],"answer":1,"dimension":"能力提升","explanation":"照片记录的是画面和图形，属于图像数据。"},{"id":"s1","stem":"记录植物生长变化的连续录像，主要属于哪类数据？","options":["视频数据","文字数据","声音数据","数值数据"],"answer":0,"dimension":"素养变化","explanation":"视频是连续变化的画面，常常还带有声音。"},{"id":"s2","stem":"“班级有 42 名同学”中的“42”主要属于哪类数据？","options":["图像数据","文字数据","数值数据","声音数据"],"answer":2,"dimension":"素养变化","explanation":"表示数量的数字属于数值数据。"}]}}',
    t.max_score = 100
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 泛在的数据' AND cl.title = '第1课 身边的数据'
  AND t.task_type = 'EXTERNAL';
