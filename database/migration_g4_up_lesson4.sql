-- 四年级上册第4课《数据的安全》
SET NAMES utf8mb4;

UPDATE course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET cl.content = '<p>本课学习<strong>数据的安全</strong>：生成数字名片并识别敏感个人数据，了解商业数据泄露的风险，并认识数据安全与国家安全的关系。</p><p>请从左侧依次完成<strong>探究一、探究二、探究三</strong>，然后填写<strong>学习评价</strong>并作答<strong>课堂小测</strong>。</p>'
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 泛在的数据' AND cl.title = '第4课 数据的安全'
  AND (cl.content IS NULL OR cl.content LIKE '%请按教师课堂安排完成学习%');

INSERT INTO course_resource (lesson_id, title, res_type, content_url, sort_order)
SELECT cl.id, '数据的安全探究活动', 'WEB', '/lessons/g4-up-lesson4/index.html', 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 泛在的数据' AND cl.title = '第4课 数据的安全'
  AND NOT EXISTS (
    SELECT 1 FROM course_resource r
    WHERE r.lesson_id = cl.id AND r.res_type = 'WEB' AND r.content_url = '/lessons/g4-up-lesson4/index.html'
  )
LIMIT 1;

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order)
SELECT cl.id, '数据的安全探究', '四年级上第4课学生工作台：探究一数字名片、探究二商业数据、探究三国家安全、学习评价、课堂小测', 'EXTERNAL',
  '{"layout":"student_workspace","lessonTitle":"第4课 数据的安全","objectives":"我能说出哪些个人信息要保护，知道企业怎样保护商业数据，并了解数据安全与国家安全的关系。","evaluationHint":"完成三个探究后，请如实填写学习评价，再作答课堂小测。","introMap":{"title":"数据的安全","branches":[{"label":"3 本课学习目标","nodes":[{"text":"保护姓名、生日、住址等敏感数据"},{"text":"知道商业数据泄露的风险"},{"text":"了解数据安全与国家安全"}]},{"label":"1. 核心学习内容","nodes":[{"text":"个人数据不能随便告诉陌生人"},{"text":"不点击来源不明的链接"},{"text":"数据安全法保护数据安全"}]},{"label":"2. 课堂完成顺序","nodes":[{"text":"探究一：我的数字名片"},{"text":"探究二：商业数据小侦探"},{"text":"探究三：国家安全守护者"},{"text":"完成学习评价和课堂小测"}]}]},"evaluationForm":{"title":"学习评价","intro":"以下题目没有标准答案，请根据本课真实学习感受选择最符合的一项。","questions":[{"id":"e1","aspect":"学习兴趣","stem":"本课「数据的安全」的内容，你的学习兴趣如何？","options":["非常感兴趣，还想再了解怎样保护数据","比较感兴趣，愿意完成探究活动","一般，能跟着完成要求","不太感兴趣，觉得有点难"]},{"id":"e2","aspect":"难度感知","stem":"你觉得本课哪个探究最有挑战性？","options":["探究一：生成数字名片，并判断敏感个人数据","探究二：分辨商业数据泄露和诈骗","探究三：理解气象数据与国家安全","我觉得都不难，掌握较好"]},{"id":"e3","aspect":"学习信心","stem":"完成三个探究后，你对自己「保护数据安全」的信心如何？","options":["非常有信心，能提醒家人和同学","比较有信心，偶尔需要提示","信心一般，还需要多练习","还不太有信心，希望获得更多帮助"]}]},"activities":[{"index":1,"title":"我的数字名片","step":1,"path":"/lessons/g4-up-lesson4/index.html","unlocked":true},{"index":2,"title":"商业数据小侦探","step":2,"path":"/lessons/g4-up-lesson4/index.html","unlocked":false},{"index":3,"title":"国家安全守护者","step":3,"path":"/lessons/g4-up-lesson4/index.html","unlocked":false}],"quiz":{"title":"课堂小测","unlocked":false,"unlockAfterActivity":3,"questions":[{"id":"k1","stem":"下列哪一项属于敏感个人数据？","options":["喜欢的颜色","真实姓名和出生日期","读几年级","喜欢的课"],"answer":1,"dimension":"知识掌握","explanation":"姓名和出生日期可以标识一个人，属于敏感个人数据，不能随便告诉陌生人。"},{"id":"k2","stem":"陌生人在网上问你家住址，比较安全的做法是？","options":["告诉详细地址","发送家门口照片","礼貌拒绝，不透露住址","告诉小区名字和楼栋"],"answer":2,"dimension":"知识掌握","explanation":"家庭住址泄露后可能带来现实中的安全风险，应当拒绝告诉陌生人。"},{"id":"a1","stem":"购物网站数据被窃取，最需要担心的是？","options":["网页颜色变了","广告变少了","账号密码和支付信息被盗用","商品图片不清晰"],"answer":2,"dimension":"能力提升","explanation":"商业数据泄露的主要风险是用户隐私和财产安全。"},{"id":"a2","stem":"网站要求输入银行卡密码才能领免费礼品，这通常是？","options":["正规福利","常见的网络诈骗","银行的正式通知","学校布置的作业"],"answer":1,"dimension":"能力提升","explanation":"索要银行卡密码的“免费礼品”通常是诈骗，不能输入密码。"},{"id":"s1","stem":"为什么气象数据也需要保护？","options":["因为它只属于天气预报员","因为它可能关系国家安全","因为收集它非常好玩","因为每天都会变化"],"answer":1,"dimension":"素养变化","explanation":"特定地区的气象、地理数据如果被恶意收集，可能涉及国家安全。"},{"id":"s2","stem":"《中华人民共和国数据安全法》主要想做到什么？","options":["禁止大家上网","规范数据处理，保障数据安全","让所有数据都收费","只保护游戏账号"],"answer":1,"dimension":"素养变化","explanation":"数据安全法从 2021 年 9 月 1 日起施行，用来规范数据处理并保障数据安全。"}]}}',
  100, 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 泛在的数据' AND cl.title = '第4课 数据的安全'
  AND NOT EXISTS (
    SELECT 1 FROM course_task t WHERE t.lesson_id = cl.id AND t.task_type = 'EXTERNAL'
  )
LIMIT 1;

UPDATE course_task t
INNER JOIN course_lesson cl ON t.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
SET t.title = '数据的安全探究',
    t.description = '四年级上第4课学生工作台：探究一数字名片、探究二商业数据、探究三国家安全、学习评价、课堂小测',
    t.config_json = '{"layout":"student_workspace","lessonTitle":"第4课 数据的安全","objectives":"我能说出哪些个人信息要保护，知道企业怎样保护商业数据，并了解数据安全与国家安全的关系。","evaluationHint":"完成三个探究后，请如实填写学习评价，再作答课堂小测。","introMap":{"title":"数据的安全","branches":[{"label":"3 本课学习目标","nodes":[{"text":"保护姓名、生日、住址等敏感数据"},{"text":"知道商业数据泄露的风险"},{"text":"了解数据安全与国家安全"}]},{"label":"1. 核心学习内容","nodes":[{"text":"个人数据不能随便告诉陌生人"},{"text":"不点击来源不明的链接"},{"text":"数据安全法保护数据安全"}]},{"label":"2. 课堂完成顺序","nodes":[{"text":"探究一：我的数字名片"},{"text":"探究二：商业数据小侦探"},{"text":"探究三：国家安全守护者"},{"text":"完成学习评价和课堂小测"}]}]},"evaluationForm":{"title":"学习评价","intro":"以下题目没有标准答案，请根据本课真实学习感受选择最符合的一项。","questions":[{"id":"e1","aspect":"学习兴趣","stem":"本课「数据的安全」的内容，你的学习兴趣如何？","options":["非常感兴趣，还想再了解怎样保护数据","比较感兴趣，愿意完成探究活动","一般，能跟着完成要求","不太感兴趣，觉得有点难"]},{"id":"e2","aspect":"难度感知","stem":"你觉得本课哪个探究最有挑战性？","options":["探究一：生成数字名片，并判断敏感个人数据","探究二：分辨商业数据泄露和诈骗","探究三：理解气象数据与国家安全","我觉得都不难，掌握较好"]},{"id":"e3","aspect":"学习信心","stem":"完成三个探究后，你对自己「保护数据安全」的信心如何？","options":["非常有信心，能提醒家人和同学","比较有信心，偶尔需要提示","信心一般，还需要多练习","还不太有信心，希望获得更多帮助"]}]},"activities":[{"index":1,"title":"我的数字名片","step":1,"path":"/lessons/g4-up-lesson4/index.html","unlocked":true},{"index":2,"title":"商业数据小侦探","step":2,"path":"/lessons/g4-up-lesson4/index.html","unlocked":false},{"index":3,"title":"国家安全守护者","step":3,"path":"/lessons/g4-up-lesson4/index.html","unlocked":false}],"quiz":{"title":"课堂小测","unlocked":false,"unlockAfterActivity":3,"questions":[{"id":"k1","stem":"下列哪一项属于敏感个人数据？","options":["喜欢的颜色","真实姓名和出生日期","读几年级","喜欢的课"],"answer":1,"dimension":"知识掌握","explanation":"姓名和出生日期可以标识一个人，属于敏感个人数据，不能随便告诉陌生人。"},{"id":"k2","stem":"陌生人在网上问你家住址，比较安全的做法是？","options":["告诉详细地址","发送家门口照片","礼貌拒绝，不透露住址","告诉小区名字和楼栋"],"answer":2,"dimension":"知识掌握","explanation":"家庭住址泄露后可能带来现实中的安全风险，应当拒绝告诉陌生人。"},{"id":"a1","stem":"购物网站数据被窃取，最需要担心的是？","options":["网页颜色变了","广告变少了","账号密码和支付信息被盗用","商品图片不清晰"],"answer":2,"dimension":"能力提升","explanation":"商业数据泄露的主要风险是用户隐私和财产安全。"},{"id":"a2","stem":"网站要求输入银行卡密码才能领免费礼品，这通常是？","options":["正规福利","常见的网络诈骗","银行的正式通知","学校布置的作业"],"answer":1,"dimension":"能力提升","explanation":"索要银行卡密码的“免费礼品”通常是诈骗，不能输入密码。"},{"id":"s1","stem":"为什么气象数据也需要保护？","options":["因为它只属于天气预报员","因为它可能关系国家安全","因为收集它非常好玩","因为每天都会变化"],"answer":1,"dimension":"素养变化","explanation":"特定地区的气象、地理数据如果被恶意收集，可能涉及国家安全。"},{"id":"s2","stem":"《中华人民共和国数据安全法》主要想做到什么？","options":["禁止大家上网","规范数据处理，保障数据安全","让所有数据都收费","只保护游戏账号"],"answer":1,"dimension":"素养变化","explanation":"数据安全法从 2021 年 9 月 1 日起施行，用来规范数据处理并保障数据安全。"}]}}',
    t.max_score = 100
WHERE cg.name = '四年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 泛在的数据' AND cl.title = '第4课 数据的安全'
  AND t.task_type = 'EXTERNAL'
  AND (t.config_json IS NULL OR t.config_json NOT LIKE '%我的数字名片%');
