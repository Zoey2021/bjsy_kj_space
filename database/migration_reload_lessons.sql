-- 仅重建配套教材课时（保留 course_unit 不动）
-- 适用：单元名称已对，但课时仍串课/是旧数据

USE learn_space;

DELETE ls FROM learn_submission ls
INNER JOIN course_task ct ON ls.task_id = ct.id
INNER JOIN course_lesson cl ON ct.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.textbook_type = 'MAIN';

DELETE lpt FROM learn_points lpt
INNER JOIN course_task ct ON lpt.source_type = 'TASK' AND lpt.source_id = ct.id
INNER JOIN course_lesson cl ON ct.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.textbook_type = 'MAIN';

DELETE lp FROM learn_progress lp
INNER JOIN course_lesson cl ON lp.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.textbook_type = 'MAIN';

DELETE lv FROM learn_visit_log lv
INNER JOIN course_lesson cl ON lv.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.textbook_type = 'MAIN';

DELETE cr FROM course_resource cr
INNER JOIN course_lesson cl ON cr.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.textbook_type = 'MAIN';

DELETE ct FROM course_task ct
INNER JOIN course_lesson cl ON ct.lesson_id = cl.id
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.textbook_type = 'MAIN';

DELETE cl FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.textbook_type = 'MAIN';

DELETE FROM course_lesson WHERE unit_id NOT IN (SELECT id FROM course_unit);

ALTER TABLE course_lesson AUTO_INCREMENT = 1;

INSERT INTO course_lesson (unit_id, title, sort_order, content, duration_min) VALUES
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 感受信息社会' LIMIT 1), '第1课 认识在线社会', 1, '<p>本课属于《三年级上册》《第一单元 感受信息社会》。</p><p>教材页码约第 2 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 感受信息社会' LIMIT 1), '第2课 感知智能生活', 2, '<p>本课属于《三年级上册》《第一单元 感受信息社会》。</p><p>教材页码约第 6 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 感受信息社会' LIMIT 1), '第3课 了解信息处理工具', 3, '<p>本课属于《三年级上册》《第一单元 感受信息社会》。</p><p>教材页码约第 9 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 获取在线资源' LIMIT 1), '第4课 进入在线平台', 1, '<p>本课属于《三年级上册》《第二单元 获取在线资源》。</p><p>教材页码约第 14 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 获取在线资源' LIMIT 1), '第5课 下载平台资源', 2, '<p>本课属于《三年级上册》《第二单元 获取在线资源》。</p><p>教材页码约第 19 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 获取在线资源' LIMIT 1), '第6课 查看资源与文件', 3, '<p>本课属于《三年级上册》《第二单元 获取在线资源》。</p><p>教材页码约第 23 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 获取在线资源' LIMIT 1), '第7课 分类整理资源', 4, '<p>本课属于《三年级上册》《第二单元 获取在线资源》。</p><p>教材页码约第 27 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 获取在线资源' LIMIT 1), '第8课 共享资源途径', 5, '<p>本课属于《三年级上册》《第二单元 获取在线资源》。</p><p>教材页码约第 31 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 体验在线生活' LIMIT 1), '第9课 体验在线应用', 1, '<p>本课属于《三年级上册》《第三单元 体验在线生活》。</p><p>教材页码约第 36 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 体验在线生活' LIMIT 1), '第10课 绘制在线生活', 2, '<p>本课属于《三年级上册》《第三单元 体验在线生活》。</p><p>教材页码约第 40 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 体验在线生活' LIMIT 1), '第11课 关注网络痕迹', 3, '<p>本课属于《三年级上册》《第三单元 体验在线生活》。</p><p>教材页码约第 44 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 体验在线生活' LIMIT 1), '第12课 保护数字身份', 4, '<p>本课属于《三年级上册》《第三单元 体验在线生活》。</p><p>教材页码约第 48 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第四单元 开展在线学习' LIMIT 1), '第13课 分解问题步骤', 1, '<p>本课属于《三年级上册》《第四单元 开展在线学习》。</p><p>教材页码约第 52 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第四单元 开展在线学习' LIMIT 1), '第14课 在线协作学习', 2, '<p>本课属于《三年级上册》《第四单元 开展在线学习》。</p><p>教材页码约第 56 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第四单元 开展在线学习' LIMIT 1), '第15课 分享学习成果', 3, '<p>本课属于《三年级上册》《第四单元 开展在线学习》。</p><p>教材页码约第 61 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 整理数字资源' LIMIT 1), '第1课 多样的数字资源', 1, '<p>本课属于《三年级下册》《第一单元 整理数字资源》。</p><p>教材页码约第 2 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 整理数字资源' LIMIT 1), '第2课 感知媒体编码', 2, '<p>本课属于《三年级下册》《第一单元 整理数字资源》。</p><p>教材页码约第 5 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 整理数字资源' LIMIT 1), '第3课 媒体文件类型', 3, '<p>本课属于《三年级下册》《第一单元 整理数字资源》。</p><p>教材页码约第 9 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 整理数字资源' LIMIT 1), '第4课 数字资源分类', 4, '<p>本课属于《三年级下册》《第一单元 整理数字资源》。</p><p>教材页码约第 13 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 整理数字资源' LIMIT 1), '第5课 应用数字资源', 5, '<p>本课属于《三年级下册》《第一单元 整理数字资源》。</p><p>教材页码约第 17 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 创作数字作品' LIMIT 1), '第6课 数字作品面面观', 1, '<p>本课属于《三年级下册》《第二单元 创作数字作品》。</p><p>教材页码约第 22 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 创作数字作品' LIMIT 1), '第7课 处理图像素材', 2, '<p>本课属于《三年级下册》《第二单元 创作数字作品》。</p><p>教材页码约第 26 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 创作数字作品' LIMIT 1), '第8课 剪辑音频素材', 3, '<p>本课属于《三年级下册》《第二单元 创作数字作品》。</p><p>教材页码约第 30 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 创作数字作品' LIMIT 1), '第9课 编辑视频素材', 4, '<p>本课属于《三年级下册》《第二单元 创作数字作品》。</p><p>教材页码约第 34 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 创作数字作品' LIMIT 1), '第10课 创作发布作品', 5, '<p>本课属于《三年级下册》《第二单元 创作数字作品》。</p><p>教材页码约第 38 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 在线学习小能手' LIMIT 1), '第11课 在线学习工具', 1, '<p>本课属于《三年级下册》《第三单元 在线学习小能手》。</p><p>教材页码约第 44 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 在线学习小能手' LIMIT 1), '第12课 描述主题学习', 2, '<p>本课属于《三年级下册》《第三单元 在线学习小能手》。</p><p>教材页码约第 49 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 在线学习小能手' LIMIT 1), '第13课 资源收集途径', 3, '<p>本课属于《三年级下册》《第三单元 在线学习小能手》。</p><p>教材页码约第 53 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 在线学习小能手' LIMIT 1), '第14课 资源整合加工', 4, '<p>本课属于《三年级下册》《第三单元 在线学习小能手》。</p><p>教材页码约第 57 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 在线学习小能手' LIMIT 1), '第15课 成果分享交流', 5, '<p>本课属于《三年级下册》《第三单元 在线学习小能手》。</p><p>教材页码约第 61 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 泛在的数据' LIMIT 1), '第1课 身边的数据', 1, '<p>本课属于《四年级上册》《第一单元 泛在的数据》。</p><p>教材页码约第 2 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 泛在的数据' LIMIT 1), '第2课 多样的数据', 2, '<p>本课属于《四年级上册》《第一单元 泛在的数据》。</p><p>教材页码约第 5 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 泛在的数据' LIMIT 1), '第3课 数据的价值', 3, '<p>本课属于《四年级上册》《第一单元 泛在的数据》。</p><p>教材页码约第 9 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 泛在的数据' LIMIT 1), '第4课 数据的安全', 4, '<p>本课属于《四年级上册》《第一单元 泛在的数据》。</p><p>教材页码约第 13 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 数据证明观点' LIMIT 1), '第5课 数据获取', 1, '<p>本课属于《四年级上册》《第二单元 数据证明观点》。</p><p>教材页码约第 18 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 数据证明观点' LIMIT 1), '第6课 数据整理', 2, '<p>本课属于《四年级上册》《第二单元 数据证明观点》。</p><p>教材页码约第 22 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 数据证明观点' LIMIT 1), '第7课 数据计算', 3, '<p>本课属于《四年级上册》《第二单元 数据证明观点》。</p><p>教材页码约第 26 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 数据证明观点' LIMIT 1), '第8课 图表呈现', 4, '<p>本课属于《四年级上册》《第二单元 数据证明观点》。</p><p>教材页码约第 30 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 数据证明观点' LIMIT 1), '第9课 数据解读与分析', 5, '<p>本课属于《四年级上册》《第二单元 数据证明观点》。</p><p>教材页码约第 34 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 身边的编码' LIMIT 1), '第10课 从数据到编码', 1, '<p>本课属于《四年级上册》《第三单元 身边的编码》。</p><p>教材页码约第 40 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 身边的编码' LIMIT 1), '第11课 有序的世界', 2, '<p>本课属于《四年级上册》《第三单元 身边的编码》。</p><p>教材页码约第 44 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 身边的编码' LIMIT 1), '第12课 编码长度与信息量', 3, '<p>本课属于《四年级上册》《第三单元 身边的编码》。</p><p>教材页码约第 48 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 身边的编码' LIMIT 1), '第13课 数据有关联', 4, '<p>本课属于《四年级上册》《第三单元 身边的编码》。</p><p>教材页码约第 52 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 身边的编码' LIMIT 1), '第14课 编码的规则制订', 5, '<p>本课属于《四年级上册》《第三单元 身边的编码》。</p><p>教材页码约第 57 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 身边的编码' LIMIT 1), '第15课 编码的验证优化', 6, '<p>本课属于《四年级上册》《第三单元 身边的编码》。</p><p>教材页码约第 61 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 数字世界' LIMIT 1), '第1课 初探数字化', 1, '<p>本课属于《四年级下册》《第一单元 数字世界》。</p><p>教材页码约第 2 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 数字世界' LIMIT 1), '第2课 图像编码', 2, '<p>本课属于《四年级下册》《第一单元 数字世界》。</p><p>教材页码约第 6 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 数字世界' LIMIT 1), '第3课 字符编码', 3, '<p>本课属于《四年级下册》《第一单元 数字世界》。</p><p>教材页码约第 11 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 数字世界' LIMIT 1), '第4课 声音编码', 4, '<p>本课属于《四年级下册》《第一单元 数字世界》。</p><p>教材页码约第 15 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 数字世界' LIMIT 1), '第5课 视频编码', 5, '<p>本课属于《四年级下册》《第一单元 数字世界》。</p><p>教材页码约第 20 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 解码与校验' LIMIT 1), '第6课 数据解码', 1, '<p>本课属于《四年级下册》《第二单元 解码与校验》。</p><p>教材页码约第 26 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 解码与校验' LIMIT 1), '第7课 数据校验', 2, '<p>本课属于《四年级下册》《第二单元 解码与校验》。</p><p>教材页码约第 29 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 解码与校验' LIMIT 1), '第8课 数据需要保护', 3, '<p>本课属于《四年级下册》《第二单元 解码与校验》。</p><p>教材页码约第 33 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 解码与校验' LIMIT 1), '第9课 人机编码有差异', 4, '<p>本课属于《四年级下册》《第二单元 解码与校验》。</p><p>教材页码约第 37 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 用数据讲故事' LIMIT 1), '第10课 记录身边的数据', 1, '<p>本课属于《四年级下册》《第三单元 用数据讲故事》。</p><p>教材页码约第 42 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 用数据讲故事' LIMIT 1), '第11课 设计统计表', 2, '<p>本课属于《四年级下册》《第三单元 用数据讲故事》。</p><p>教材页码约第 45 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 用数据讲故事' LIMIT 1), '第12课 数据可视化', 3, '<p>本课属于《四年级下册》《第三单元 用数据讲故事》。</p><p>教材页码约第 49 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 用数据讲故事' LIMIT 1), '第13课 规律与预测', 4, '<p>本课属于《四年级下册》《第三单元 用数据讲故事》。</p><p>教材页码约第 53 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 用数据讲故事' LIMIT 1), '第14课 数据分析报告', 5, '<p>本课属于《四年级下册》《第三单元 用数据讲故事》。</p><p>教材页码约第 57 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='四年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 用数据讲故事' LIMIT 1), '第15课 分享数据故事', 6, '<p>本课属于《四年级下册》《第三单元 用数据讲故事》。</p><p>教材页码约第 62 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法与算法表示' LIMIT 1), '第1课 身边的算法', 1, '<p>本课属于《五年级上册》《第一单元 算法与算法表示》。</p><p>教材页码约第 2 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法与算法表示' LIMIT 1), '第2课 自然语言描述算法', 2, '<p>本课属于《五年级上册》《第一单元 算法与算法表示》。</p><p>教材页码约第 6 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法与算法表示' LIMIT 1), '第3课 流程图描述算法', 3, '<p>本课属于《五年级上册》《第一单元 算法与算法表示》。</p><p>教材页码约第 10 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法与算法表示' LIMIT 1), '第4课 算法中的数据', 4, '<p>本课属于《五年级上册》《第一单元 算法与算法表示》。</p><p>教材页码约第 15 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法与算法表示' LIMIT 1), '第5课 算法的特征', 5, '<p>本课属于《五年级上册》《第一单元 算法与算法表示》。</p><p>教材页码约第 20 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的控制结构' LIMIT 1), '第6课 顺序结构', 1, '<p>本课属于《五年级上册》《第二单元 算法的控制结构》。</p><p>教材页码约第 24 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的控制结构' LIMIT 1), '第7课 分支结构', 2, '<p>本课属于《五年级上册》《第二单元 算法的控制结构》。</p><p>教材页码约第 28 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的控制结构' LIMIT 1), '第8课 双分支结构', 3, '<p>本课属于《五年级上册》《第二单元 算法的控制结构》。</p><p>教材页码约第 31 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的控制结构' LIMIT 1), '第9课 体验算法控制', 4, '<p>本课属于《五年级上册》《第二单元 算法的控制结构》。</p><p>教材页码约第 35 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 用算法解决问题' LIMIT 1), '第10课 问题的分解', 1, '<p>本课属于《五年级上册》《第三单元 用算法解决问题》。</p><p>教材页码约第 40 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 用算法解决问题' LIMIT 1), '第11课 问题的抽象', 2, '<p>本课属于《五年级上册》《第三单元 用算法解决问题》。</p><p>教材页码约第 44 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 用算法解决问题' LIMIT 1), '第12课 模型的建立', 3, '<p>本课属于《五年级上册》《第三单元 用算法解决问题》。</p><p>教材页码约第 49 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 用算法解决问题' LIMIT 1), '第13课 算法的设计', 4, '<p>本课属于《五年级上册》《第三单元 用算法解决问题》。</p><p>教材页码约第 53 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 用算法解决问题' LIMIT 1), '第14课 算法的验证', 5, '<p>本课属于《五年级上册》《第三单元 用算法解决问题》。</p><p>教材页码约第 57 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 用算法解决问题' LIMIT 1), '第15课 算法的应用', 6, '<p>本课属于《五年级上册》《第三单元 用算法解决问题》。</p><p>教材页码约第 61 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 生活中的系统' LIMIT 1), '第1课 身边的系统', 1, '<p>本课属于《五年级下册》《第一单元 生活中的系统》。</p><p>教材页码约第 2 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 生活中的系统' LIMIT 1), '第2课 系统的构成', 2, '<p>本课属于《五年级下册》《第一单元 生活中的系统》。</p><p>教材页码约第 7 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 生活中的系统' LIMIT 1), '第3课 观察系统', 3, '<p>本课属于《五年级下册》《第一单元 生活中的系统》。</p><p>教材页码约第 11 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 控制系统' LIMIT 1), '第4课 生活中的控制系统', 1, '<p>本课属于《五年级下册》《第二单元 控制系统》。</p><p>教材页码约第 16 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 控制系统' LIMIT 1), '第5课 控制系统的三个环节', 2, '<p>本课属于《五年级下册》《第二单元 控制系统》。</p><p>教材页码约第 20 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 控制系统' LIMIT 1), '第6课 控制系统的输入', 3, '<p>本课属于《五年级下册》《第二单元 控制系统》。</p><p>教材页码约第 23 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 控制系统' LIMIT 1), '第7课 控制系统的输出', 4, '<p>本课属于《五年级下册》《第二单元 控制系统》。</p><p>教材页码约第 27 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 控制系统' LIMIT 1), '第8课 体验控制系统', 5, '<p>本课属于《五年级下册》《第二单元 控制系统》。</p><p>教材页码约第 31 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 控制系统中的计算' LIMIT 1), '第9课 控制系统中的计算', 1, '<p>本课属于《五年级下册》《第三单元 控制系统中的计算》。</p><p>教材页码约第 36 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 控制系统中的计算' LIMIT 1), '第10课 计算机在控制系统中的作用', 2, '<p>本课属于《五年级下册》《第三单元 控制系统中的计算》。</p><p>教材页码约第 39 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 控制系统中的计算' LIMIT 1), '第11课 查表计算', 3, '<p>本课属于《五年级下册》《第三单元 控制系统中的计算》。</p><p>教材页码约第 42 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 控制系统中的计算' LIMIT 1), '第12课 条件计算', 4, '<p>本课属于《五年级下册》《第三单元 控制系统中的计算》。</p><p>教材页码约第 47 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 控制系统中的计算' LIMIT 1), '第13课 循环结构（一）', 5, '<p>本课属于《五年级下册》《第三单元 控制系统中的计算》。</p><p>教材页码约第 51 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 控制系统中的计算' LIMIT 1), '第14课 循环结构（二）', 6, '<p>本课属于《五年级下册》《第三单元 控制系统中的计算》。</p><p>教材页码约第 55 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='五年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 控制系统中的计算' LIMIT 1), '第15课 恒温箱实验', 7, '<p>本课属于《五年级下册》《第三单元 控制系统中的计算》。</p><p>教材页码约第 59 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第1课 算法与问题解决', 1, '<p>本课属于《六年级上册》《第一单元 算法的实现》。</p><p>教材页码约第 2 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第2课 抽象与建模', 2, '<p>本课属于《六年级上册》《第一单元 算法的实现》。</p><p>教材页码约第 7 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第3课 算法设计', 3, '<p>本课属于《六年级上册》《第一单元 算法的实现》。</p><p>教材页码约第 10 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第4课 算法的程序体验', 4, '<p>本课属于《六年级上册》《第一单元 算法的实现》。</p><p>教材页码约第 14 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第5课 算法的执行', 5, '<p>本课属于《六年级上册》《第一单元 算法的实现》。</p><p>教材页码约第 18 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第6课 猜数字算法设计', 6, '<p>本课属于《六年级上册》《第一单元 算法的实现》。</p><p>教材页码约第 24 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第7课 猜数字算法验证', 7, '<p>本课属于《六年级上册》《第一单元 算法的实现》。</p><p>教材页码约第 28 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的效率' LIMIT 1), '第8课 算法的多样性', 1, '<p>本课属于《六年级上册》《第二单元 算法的效率》。</p><p>教材页码约第 34 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的效率' LIMIT 1), '第9课 算法的评价方法', 2, '<p>本课属于《六年级上册》《第二单元 算法的效率》。</p><p>教材页码约第 40 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的效率' LIMIT 1), '第10课 “韩信点兵”枚举法的实现', 3, '<p>本课属于《六年级上册》《第二单元 算法的效率》。</p><p>教材页码约第 43 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的效率' LIMIT 1), '第11课 “韩信点兵”筛选法的实现', 4, '<p>本课属于《六年级上册》《第二单元 算法的效率》。</p><p>教材页码约第 47 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的效率' LIMIT 1), '第12课 “韩信点兵”同余法的实现', 5, '<p>本课属于《六年级上册》《第二单元 算法的效率》。</p><p>教材页码约第 51 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 算法的影响' LIMIT 1), '第13课 在线生活中的算法', 1, '<p>本课属于《六年级上册》《第三单元 算法的影响》。</p><p>教材页码约第 56 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 算法的影响' LIMIT 1), '第14课 算法对生活的影响', 2, '<p>本课属于《六年级上册》《第三单元 算法的影响》。</p><p>教材页码约第 60 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 算法的影响' LIMIT 1), '第15课 人机对话的实现', 3, '<p>本课属于《六年级上册》《第三单元 算法的影响》。</p><p>教材页码约第 63 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 控制系统中的反馈' LIMIT 1), '第1课 自动控制系统', 1, '<p>本课属于《六年级下册》《第一单元 控制系统中的反馈》。</p><p>教材页码约第 2 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 控制系统中的反馈' LIMIT 1), '第2课 控制的形态', 2, '<p>本课属于《六年级下册》《第一单元 控制系统中的反馈》。</p><p>教材页码约第 6 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 控制系统中的反馈' LIMIT 1), '第3课 反馈与控制', 3, '<p>本课属于《六年级下册》《第一单元 控制系统中的反馈》。</p><p>教材页码约第 10 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 控制系统中的反馈' LIMIT 1), '第4课 反馈的实现', 4, '<p>本课属于《六年级下册》《第一单元 控制系统中的反馈》。</p><p>教材页码约第 13 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 控制系统中的运算' LIMIT 1), '第5课 控制系统中的数据', 1, '<p>本课属于《六年级下册》《第二单元 控制系统中的运算》。</p><p>教材页码约第 18 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 控制系统中的运算' LIMIT 1), '第6课 连续量与开关量', 2, '<p>本课属于《六年级下册》《第二单元 控制系统中的运算》。</p><p>教材页码约第 22 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 控制系统中的运算' LIMIT 1), '第7课 开关量的生成', 3, '<p>本课属于《六年级下册》《第二单元 控制系统中的运算》。</p><p>教材页码约第 26 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 控制系统中的运算' LIMIT 1), '第8课 开关量的与运算', 4, '<p>本课属于《六年级下册》《第二单元 控制系统中的运算》。</p><p>教材页码约第 30 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 控制系统中的运算' LIMIT 1), '第9课 开关量的或运算', 5, '<p>本课属于《六年级下册》《第二单元 控制系统中的运算》。</p><p>教材页码约第 34 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 控制系统中的运算' LIMIT 1), '第10课 开关量与控制', 6, '<p>本课属于《六年级下册》《第二单元 控制系统中的运算》。</p><p>教材页码约第 38 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 扩音系统' LIMIT 1), '第11课 身边的扩音系统', 1, '<p>本课属于《六年级下册》《第三单元 扩音系统》。</p><p>教材页码约第 44 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 扩音系统' LIMIT 1), '第12课 计算机控制的扩音系统', 2, '<p>本课属于《六年级下册》《第三单元 扩音系统》。</p><p>教材页码约第 48 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 扩音系统' LIMIT 1), '第13课 扩音系统的控制', 3, '<p>本课属于《六年级下册》《第三单元 扩音系统》。</p><p>教材页码约第 52 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 扩音系统' LIMIT 1), '第14课 扩音系统的优化', 4, '<p>本课属于《六年级下册》《第三单元 扩音系统》。</p><p>教材页码约第 56 页。请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级下册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 扩音系统' LIMIT 1), '第15课 控制系统的安全', 5, '<p>本课属于《六年级下册》《第三单元 扩音系统》。</p><p>教材页码约第 60 页。请按教师课堂安排完成学习。</p>', 40);

INSERT INTO course_resource (lesson_id, title, res_type, content_text, sort_order) VALUES
((SELECT cl.id FROM course_lesson cl INNER JOIN course_unit cu ON cl.unit_id = cu.id INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='三年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 感受信息社会' AND cl.sort_order = 1 LIMIT 1), '单元导学', 'TEXT', '<p>欢迎学习本册信息科技课程，请按单元顺序完成各课时。</p>', 1);
