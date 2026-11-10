-- 四年级上册第7课《数据计算》：三个探究 + 学习评价 + 课堂小测
-- 可在现有库执行；后端启动时 G4UpLesson7Seeder 也会自动绑定/更新。

SET NAMES utf8mb4;

UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.content = '<p>本课学习<strong>数据计算</strong>：认识单元格地址和区域，用求和公式让表格自动计算，并在 WPS 表格中完成成绩表后上传。</p><p>请从左侧依次完成<strong>探究一、探究二、探究三</strong>，然后作答<strong>课堂小测</strong>并填写<strong>学习评价</strong>。</p>'
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第二单元 数据证明观点' AND cl.title = '第7课 数据计算'
  AND (cl.content IS NULL OR cl.content LIKE '%请按教师课堂安排完成学习%');

INSERT INTO course_resource (lesson_id, title, res_type, content_url, sort_order)
SELECT cl.id, '数据计算探究活动', 'WEB', '/lessons/g4-up-lesson7/index.html', 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第二单元 数据证明观点' AND cl.title = '第7课 数据计算'
  AND NOT EXISTS (
    SELECT 1 FROM course_resource r
    WHERE r.lesson_id = cl.id AND r.res_type = 'WEB' AND r.content_url = '/lessons/g4-up-lesson7/index.html'
  )
LIMIT 1;

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order)
SELECT cl.id, '数据计算探究',
  '四年级上第7课学生工作台：探究一找格子、探究二会计算、探究三交作品、学习评价、课堂小测',
  'EXTERNAL', '{"layout":"student_workspace","lessonTitle":"第7课 数据计算","objectives":"我能说出单元格地址和区域写法，会用求和公式让表格自动计算，并能上传自己做好的成绩表。","evaluationHint":"完成三个探究后，请先作答课堂小测，再填写学习评价。","introMap":{"title":"数据计算","branches":[{"label":"3 本课学习目标","nodes":[{"text":"认识单元格地址和区域（如 B3、B3:E3）"},{"text":"会用 Σ、SUM 或加减公式计算"},{"text":"在 WPS 表格里完成成绩表并上传"}]},{"label":"1. 核心学习内容","nodes":[{"text":"列用字母、行用数字，合起来是单元格地址"},{"text":"冒号表示一片连续区域"},{"text":"用公式计算，数据一改结果会自动更新"}]},{"label":"2. 课堂完成顺序","nodes":[{"text":"探究一：找格子"},{"text":"探究二：会计算"},{"text":"探究三：交作品"},{"text":"完成课堂小测和学习评价"}]}]},"evaluationForm":{"title":"学习评价","intro":"以下题目没有标准答案，请根据本课真实学习感受选择最符合的一项。","questions":[{"id":"e1","aspect":"学习兴趣","stem":"本课「数据计算」的内容，你的学习兴趣如何？","options":["非常感兴趣，还想自己再算一张表","比较感兴趣，愿意完成探究活动","一般，能跟着完成要求","不太感兴趣，觉得有点难"]},{"id":"e2","aspect":"难度感知","stem":"你觉得本课哪个探究最有挑战性？","options":["探究一：找出单元格和区域","探究二：拼公式、用填充柄","探究三：在 WPS 里做表并上传","我觉得都不难，掌握较好"]},{"id":"e3","aspect":"学习信心","stem":"完成三个探究后，你对自己用表格自动计算的信心如何？","options":["非常有信心，能独立写出求和公式","比较有信心，偶尔需要提示","信心一般，还需要多练习","还不太有信心，希望获得更多帮助"]}]},"activities":[{"index":1,"title":"找格子","step":2,"path":"/lessons/g4-up-lesson7/index.html","unlocked":true},{"index":2,"title":"会计算","step":3,"path":"/lessons/g4-up-lesson7/index.html","unlocked":true},{"index":3,"title":"交作品","step":4,"path":"/lessons/g4-up-lesson7/index.html","unlocked":true}],"quiz":{"title":"课堂小测","unlocked":false,"unlockAfterActivity":3,"questions":[{"id":"k1","stem":"单元格地址 B3 表示什么？","options":["第 3 列第 B 行","B 列第 3 行","第 3 张表的 B 格","文件名是 B3"],"answer":1,"dimension":"知识掌握","explanation":"列用字母、行用数字。B3 就是 B 列第 3 行那个格子。"},{"id":"k2","stem":"写法 B3:E3 表示什么？","options":["只有 B3 和 E3 两个格子","从 B3 到 E3 连成的一片区域","第 3 行第 3 列","把 B3 改成 E3"],"answer":1,"dimension":"知识掌握","explanation":"冒号表示“从这里到那里”，B3:E3 是同一行上连续 4 个格子。"},{"id":"a1","stem":"要把 B3 到 E3 的分数加起来，比较合适的公式是？","options":["=SUM(B3:E3)","SUM(B3:E3)","B3+E3","=B3:E3"],"answer":0,"dimension":"能力提升","explanation":"公式要以等号开头，SUM 用来求和，区域写在括号里。"},{"id":"a2","stem":"为什么更推荐用公式算，而不是直接把得数填进格子？","options":["公式看起来比较长","原始数据一改，结果会自动更新","直接填数字会让软件坏掉","老师不让用手算"],"answer":1,"dimension":"能力提升","explanation":"用公式后，改了原始分数，总分和平均分会跟着变。"},{"id":"s1","stem":"上传作品时，比较合适的文件格式是？","options":[".et（WPS 默认）",".xlsx（Excel 文件）",".csv",".jpg 截图"],"answer":1,"dimension":"素养变化","explanation":"网页能检查的是 .xlsx。WPS 做好后请另存为 Excel 文件再上传。"},{"id":"s2","stem":"平均分如果要保留一位小数，应该怎么做？","options":["把数字手写短一点","选中格子，把小数位数设为 1 位","多空一格","改成整数就行"],"answer":1,"dimension":"素养变化","explanation":"用单元格的小数位数设置，可以把平均分显示成一位小数。"}]}}', 100, 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第二单元 数据证明观点' AND cl.title = '第7课 数据计算'
  AND NOT EXISTS (
    SELECT 1 FROM course_task t WHERE t.lesson_id = cl.id AND t.task_type = 'EXTERNAL'
  )
LIMIT 1;

UPDATE course_task t
INNER JOIN course_lesson cl ON t.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET t.title = '数据计算探究',
    t.description = '四年级上第7课学生工作台：探究一找格子、探究二会计算、探究三交作品、学习评价、课堂小测',
    t.config_json = '{"layout":"student_workspace","lessonTitle":"第7课 数据计算","objectives":"我能说出单元格地址和区域写法，会用求和公式让表格自动计算，并能上传自己做好的成绩表。","evaluationHint":"完成三个探究后，请先作答课堂小测，再填写学习评价。","introMap":{"title":"数据计算","branches":[{"label":"3 本课学习目标","nodes":[{"text":"认识单元格地址和区域（如 B3、B3:E3）"},{"text":"会用 Σ、SUM 或加减公式计算"},{"text":"在 WPS 表格里完成成绩表并上传"}]},{"label":"1. 核心学习内容","nodes":[{"text":"列用字母、行用数字，合起来是单元格地址"},{"text":"冒号表示一片连续区域"},{"text":"用公式计算，数据一改结果会自动更新"}]},{"label":"2. 课堂完成顺序","nodes":[{"text":"探究一：找格子"},{"text":"探究二：会计算"},{"text":"探究三：交作品"},{"text":"完成课堂小测和学习评价"}]}]},"evaluationForm":{"title":"学习评价","intro":"以下题目没有标准答案，请根据本课真实学习感受选择最符合的一项。","questions":[{"id":"e1","aspect":"学习兴趣","stem":"本课「数据计算」的内容，你的学习兴趣如何？","options":["非常感兴趣，还想自己再算一张表","比较感兴趣，愿意完成探究活动","一般，能跟着完成要求","不太感兴趣，觉得有点难"]},{"id":"e2","aspect":"难度感知","stem":"你觉得本课哪个探究最有挑战性？","options":["探究一：找出单元格和区域","探究二：拼公式、用填充柄","探究三：在 WPS 里做表并上传","我觉得都不难，掌握较好"]},{"id":"e3","aspect":"学习信心","stem":"完成三个探究后，你对自己用表格自动计算的信心如何？","options":["非常有信心，能独立写出求和公式","比较有信心，偶尔需要提示","信心一般，还需要多练习","还不太有信心，希望获得更多帮助"]}]},"activities":[{"index":1,"title":"找格子","step":2,"path":"/lessons/g4-up-lesson7/index.html","unlocked":true},{"index":2,"title":"会计算","step":3,"path":"/lessons/g4-up-lesson7/index.html","unlocked":true},{"index":3,"title":"交作品","step":4,"path":"/lessons/g4-up-lesson7/index.html","unlocked":true}],"quiz":{"title":"课堂小测","unlocked":false,"unlockAfterActivity":3,"questions":[{"id":"k1","stem":"单元格地址 B3 表示什么？","options":["第 3 列第 B 行","B 列第 3 行","第 3 张表的 B 格","文件名是 B3"],"answer":1,"dimension":"知识掌握","explanation":"列用字母、行用数字。B3 就是 B 列第 3 行那个格子。"},{"id":"k2","stem":"写法 B3:E3 表示什么？","options":["只有 B3 和 E3 两个格子","从 B3 到 E3 连成的一片区域","第 3 行第 3 列","把 B3 改成 E3"],"answer":1,"dimension":"知识掌握","explanation":"冒号表示“从这里到那里”，B3:E3 是同一行上连续 4 个格子。"},{"id":"a1","stem":"要把 B3 到 E3 的分数加起来，比较合适的公式是？","options":["=SUM(B3:E3)","SUM(B3:E3)","B3+E3","=B3:E3"],"answer":0,"dimension":"能力提升","explanation":"公式要以等号开头，SUM 用来求和，区域写在括号里。"},{"id":"a2","stem":"为什么更推荐用公式算，而不是直接把得数填进格子？","options":["公式看起来比较长","原始数据一改，结果会自动更新","直接填数字会让软件坏掉","老师不让用手算"],"answer":1,"dimension":"能力提升","explanation":"用公式后，改了原始分数，总分和平均分会跟着变。"},{"id":"s1","stem":"上传作品时，比较合适的文件格式是？","options":[".et（WPS 默认）",".xlsx（Excel 文件）",".csv",".jpg 截图"],"answer":1,"dimension":"素养变化","explanation":"网页能检查的是 .xlsx。WPS 做好后请另存为 Excel 文件再上传。"},{"id":"s2","stem":"平均分如果要保留一位小数，应该怎么做？","options":["把数字手写短一点","选中格子，把小数位数设为 1 位","多空一格","改成整数就行"],"answer":1,"dimension":"素养变化","explanation":"用单元格的小数位数设置，可以把平均分显示成一位小数。"}]}}',
    t.max_score = 100
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第二单元 数据证明观点' AND cl.title = '第7课 数据计算'
  AND t.task_type = 'EXTERNAL'
  AND (t.config_json IS NULL OR t.config_json NOT LIKE '%找格子%');
