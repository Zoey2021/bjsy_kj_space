-- 四年级上册第2课《多样的数据》：学生工作台（环节1身份证、环节2五种形式、环节3存储介质）
-- 可在现有库执行；后端启动时 G4UpLesson2Seeder 也会自动绑定/更新。

SET NAMES utf8mb4;

UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.content = '<p>本课学习<strong>多样的数据</strong>：在同一对象上找出多种数据，辨认数字、文本、图片、音频、视频五种形式，并了解存储介质从古到今的变化。</p><p>请从左侧依次完成<strong>环节1、环节2、环节3</strong>，然后填写<strong>学习评价</strong>并作答<strong>课堂小测</strong>。</p>'
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 泛在的数据' AND cl.title = '第2课 多样的数据'
  AND (cl.content IS NULL OR cl.content LIKE '%请按教师课堂安排完成学习%');

INSERT INTO course_resource (lesson_id, title, res_type, content_url, sort_order)
SELECT cl.id, '多样的数据探究活动', 'WEB', '/lessons/g4-up-lesson2/index.html', 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 泛在的数据' AND cl.title = '第2课 多样的数据'
  AND NOT EXISTS (
    SELECT 1 FROM course_resource r
    WHERE r.lesson_id = cl.id AND r.res_type = 'WEB' AND r.content_url = '/lessons/g4-up-lesson2/index.html'
  )
LIMIT 1;

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order)
SELECT cl.id, '多样的数据探究', '四年级上第2课学生工作台：环节1身份证数据、环节2五种形式、环节3存储介质', 'EXTERNAL',
  '{"layout":"student_workspace","lessonTitle":"第2课 多样的数据","objectives":"我能在同一对象上找出多种数据，辨认五种数据形式，并理解存储介质从古到今的变化。","evaluationHint":"完成三个探究环节后，请如实填写学习评价，再作答课堂小测。","introMap":{"title":"多样的数据","branches":[{"label":"3 本课学习目标","nodes":[{"text":"在同一对象上找出多种数据"},{"text":"辨认数字、文本、图片、音频、视频"},{"text":"了解存储介质的变化"}]},{"label":"1. 核心学习内容","nodes":[{"text":"数据形式是多样的"},{"text":"存储介质越新，容量越大、形式越多"}]},{"label":"2. 课堂完成顺序","nodes":[{"text":"环节1：寻找身份证上的数据"},{"text":"环节2：辨认五种数据形式"},{"text":"环节3：存储介质的变化"},{"text":"完成学习评价和课堂小测"}]}]},"evaluationForm":{"title":"学习评价","intro":"以下题目没有标准答案，请根据本课真实学习感受选择最符合的一项。","questions":[{"id":"e1","aspect":"学习兴趣","stem":"本课「多样的数据」的内容，你的学习兴趣如何？","options":["非常感兴趣，还想再找生活中的多种数据","比较感兴趣，愿意完成探究活动","一般，能跟着完成要求","不太感兴趣，觉得有点难"]},{"id":"e2","aspect":"难度感知","stem":"你觉得本课哪个环节最有挑战性？","options":["环节1：在身份证上找出各类数据","环节2：把素材配对到五种数据形式","环节3：给存储介质按从古到今排队","我觉得都不难，掌握较好"]},{"id":"e3","aspect":"学习信心","stem":"完成三个探究环节后，你对自己「认识多样的数据」的信心如何？","options":["非常有信心，能自己举出新例子","比较有信心，偶尔需要提示","信心一般，还需要多练习","还不太有信心，希望获得更多帮助"]}]},"activities":[{"index":1,"title":"寻找身份证上的数据","step":1,"path":"/lessons/g4-up-lesson2/index.html","unlocked":true},{"index":2,"title":"辨认五种数据形式","step":2,"path":"/lessons/g4-up-lesson2/index.html","unlocked":false},{"index":3,"title":"存储介质的变化","step":3,"path":"/lessons/g4-up-lesson2/index.html","unlocked":false}],"quiz":{"title":"课堂小测","unlocked":false,"unlockAfterActivity":3,"questions":[{"id":"k1","stem":"身份证上的信息主要属于哪一类数据？","options":["只有数字","文字、数字和图片等多种形式","只有图片","只有声音"],"answer":1,"dimension":"知识掌握","explanation":"身份证上同时包含姓名、出生日期等文字和数字信息，也包含照片，因此属于多种形式的数据。"},{"id":"k2","stem":"把一段声音保存下来，最合适的数据形式是？","options":["文本","音频","数字","图片"],"answer":1,"dimension":"知识掌握","explanation":"声音需要通过录音等方式保存，最合适的数据形式是音频。"},{"id":"a1","stem":"光盘通常使用什么方式读取数据？","options":["激光读取","用绳子编连","直接刻在骨片上","只靠手写"],"answer":0,"dimension":"能力提升","explanation":"光盘利用激光读取盘片上的数字信息，不是通过手写或实体刻写来读取。"},{"id":"a2","stem":"与甲骨、竹简相比，云盘的主要特点是？","options":["只能保存少量文字","必须刻写在实体材料上","通过网络存储和访问数据","不能保存图片"],"answer":2,"dimension":"能力提升","explanation":"云盘把数据存放在网络服务器中，使用者可以通过网络登录、上传和下载数据。"},{"id":"s1","stem":"下列哪项最能说明存储介质的发展趋势？","options":["容量越来越小","越来越依赖手工刻写","容量增大、携带和共享更方便","只能保存文字"],"answer":2,"dimension":"素养变化","explanation":"随着技术发展，存储容量不断增大，设备更便携，数据共享也更加方便。"}]}}',
  100, 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 泛在的数据' AND cl.title = '第2课 多样的数据'
  AND NOT EXISTS (
    SELECT 1 FROM course_task t WHERE t.lesson_id = cl.id AND t.task_type = 'EXTERNAL'
  )
LIMIT 1;

UPDATE course_task t
INNER JOIN course_lesson cl ON t.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET t.title = '多样的数据探究',
    t.description = '四年级上第2课学生工作台：环节1身份证数据、环节2五种形式、环节3存储介质',
    t.config_json = '{"layout":"student_workspace","lessonTitle":"第2课 多样的数据","objectives":"我能在同一对象上找出多种数据，辨认五种数据形式，并理解存储介质从古到今的变化。","evaluationHint":"完成三个探究环节后，请如实填写学习评价，再作答课堂小测。","introMap":{"title":"多样的数据","branches":[{"label":"3 本课学习目标","nodes":[{"text":"在同一对象上找出多种数据"},{"text":"辨认数字、文本、图片、音频、视频"},{"text":"了解存储介质的变化"}]},{"label":"1. 核心学习内容","nodes":[{"text":"数据形式是多样的"},{"text":"存储介质越新，容量越大、形式越多"}]},{"label":"2. 课堂完成顺序","nodes":[{"text":"环节1：寻找身份证上的数据"},{"text":"环节2：辨认五种数据形式"},{"text":"环节3：存储介质的变化"},{"text":"完成学习评价和课堂小测"}]}]},"evaluationForm":{"title":"学习评价","intro":"以下题目没有标准答案，请根据本课真实学习感受选择最符合的一项。","questions":[{"id":"e1","aspect":"学习兴趣","stem":"本课「多样的数据」的内容，你的学习兴趣如何？","options":["非常感兴趣，还想再找生活中的多种数据","比较感兴趣，愿意完成探究活动","一般，能跟着完成要求","不太感兴趣，觉得有点难"]},{"id":"e2","aspect":"难度感知","stem":"你觉得本课哪个环节最有挑战性？","options":["环节1：在身份证上找出各类数据","环节2：把素材配对到五种数据形式","环节3：给存储介质按从古到今排队","我觉得都不难，掌握较好"]},{"id":"e3","aspect":"学习信心","stem":"完成三个探究环节后，你对自己「认识多样的数据」的信心如何？","options":["非常有信心，能自己举出新例子","比较有信心，偶尔需要提示","信心一般，还需要多练习","还不太有信心，希望获得更多帮助"]}]},"activities":[{"index":1,"title":"寻找身份证上的数据","step":1,"path":"/lessons/g4-up-lesson2/index.html","unlocked":true},{"index":2,"title":"辨认五种数据形式","step":2,"path":"/lessons/g4-up-lesson2/index.html","unlocked":false},{"index":3,"title":"存储介质的变化","step":3,"path":"/lessons/g4-up-lesson2/index.html","unlocked":false}],"quiz":{"title":"课堂小测","unlocked":false,"unlockAfterActivity":3,"questions":[{"id":"k1","stem":"身份证上的信息主要属于哪一类数据？","options":["只有数字","文字、数字和图片等多种形式","只有图片","只有声音"],"answer":1,"dimension":"知识掌握","explanation":"身份证上同时包含姓名、出生日期等文字和数字信息，也包含照片，因此属于多种形式的数据。"},{"id":"k2","stem":"把一段声音保存下来，最合适的数据形式是？","options":["文本","音频","数字","图片"],"answer":1,"dimension":"知识掌握","explanation":"声音需要通过录音等方式保存，最合适的数据形式是音频。"},{"id":"a1","stem":"光盘通常使用什么方式读取数据？","options":["激光读取","用绳子编连","直接刻在骨片上","只靠手写"],"answer":0,"dimension":"能力提升","explanation":"光盘利用激光读取盘片上的数字信息，不是通过手写或实体刻写来读取。"},{"id":"a2","stem":"与甲骨、竹简相比，云盘的主要特点是？","options":["只能保存少量文字","必须刻写在实体材料上","通过网络存储和访问数据","不能保存图片"],"answer":2,"dimension":"能力提升","explanation":"云盘把数据存放在网络服务器中，使用者可以通过网络登录、上传和下载数据。"},{"id":"s1","stem":"下列哪项最能说明存储介质的发展趋势？","options":["容量越来越小","越来越依赖手工刻写","容量增大、携带和共享更方便","只能保存文字"],"answer":2,"dimension":"素养变化","explanation":"随着技术发展，存储容量不断增大，设备更便携，数据共享也更加方便。"}]}}',
    t.max_score = 100
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 泛在的数据' AND cl.title = '第2课 多样的数据'
  AND t.task_type = 'EXTERNAL';
