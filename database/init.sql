-- ============================================================
-- 中小学信息科技学习空间 - MySQL 8.0 建库建表脚本
-- 字符集：utf8mb4，适合 Spring Data JPA 使用
-- 【需修改】数据库名、账号密码在 application.yml 和 docker-compose 中保持一致
--
-- Navicat 说明：若「运行 SQL 文件」全部失败，请用「新建查询」打开本文件，
-- 全选后按 F5 一次执行；或先执行下面 DROP 再整文件执行（会清空本库数据）。
-- ============================================================

CREATE DATABASE IF NOT EXISTS learn_space
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE learn_space;

-- 避免中文/JSON 乱码；Navicat 执行文件时也建议保持 UTF-8
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 用户表：学生、教师、管理员统一存储，role 字段区分角色
-- ------------------------------------------------------------
CREATE TABLE sys_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(50)  NOT NULL COMMENT '登录账号',
  password      VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
  real_name     VARCHAR(50)  NOT NULL COMMENT '真实姓名',
  role          VARCHAR(20)  NOT NULL COMMENT '角色：STUDENT/TEACHER/ADMIN',
  class_id      BIGINT       NULL     COMMENT '学生所属班级ID（教师/管理员为空）',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_role (role),
  KEY idx_class_id (class_id)
) ENGINE=InnoDB COMMENT='系统用户表';

-- ------------------------------------------------------------
-- 2. 班级表：教师与班级一对多（一个教师可教多个班）
-- ------------------------------------------------------------
CREATE TABLE sys_class (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name          VARCHAR(50)  NOT NULL COMMENT '班级名称，如七年级1班',
  grade_name    VARCHAR(20)  NOT NULL COMMENT '年级名称',
  teacher_id    BIGINT       NOT NULL COMMENT '班主任/授课教师ID',
  login_code    VARCHAR(6)   NULL COMMENT '班级登录码（6位数字）',
  login_code_expires_at DATETIME NULL COMMENT '班级码过期时间',
  current_lesson_id BIGINT   NULL COMMENT '班级当前进行中的课时',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_teacher_id (teacher_id),
  UNIQUE KEY uk_login_code (login_code)
) ENGINE=InnoDB COMMENT='班级表';

-- ------------------------------------------------------------
-- 3. 班级学生关联表（支持转班等扩展）
-- ------------------------------------------------------------
CREATE TABLE sys_class_student (
  id            BIGINT   NOT NULL AUTO_INCREMENT,
  class_id      BIGINT   NOT NULL,
  student_id    BIGINT   NOT NULL,
  joined_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_class_student (class_id, student_id),
  KEY idx_student_id (student_id)
) ENGINE=InnoDB COMMENT='班级学生关联表';

-- ------------------------------------------------------------
-- 4. 课程年级（教材顶层）
-- ------------------------------------------------------------
CREATE TABLE course_grade (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  name            VARCHAR(50)  NOT NULL COMMENT '册次名称，如三年级上册',
  sort_order      INT          NOT NULL DEFAULT 0,
  description     VARCHAR(255) NULL,
  textbook_type   VARCHAR(20)  NOT NULL DEFAULT 'MAIN' COMMENT 'MAIN配套教材 SCHOOL校本教材',
  cover_url       VARCHAR(500) NULL COMMENT '封面图路径，如 /course-covers/grade3-up.jpg',
  pdf_url         VARCHAR(500) NULL COMMENT '电子教材 PDF，如 /textbooks/grade5-down.pdf',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_textbook_type (textbook_type)
) ENGINE=InnoDB COMMENT='教材册次（配套/校本）';

-- ------------------------------------------------------------
-- 5. 课程单元
-- ------------------------------------------------------------
CREATE TABLE course_unit (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  grade_id      BIGINT       NOT NULL COMMENT '所属年级',
  name          VARCHAR(100) NOT NULL COMMENT '单元名称',
  sort_order    INT          NOT NULL DEFAULT 0,
  description   VARCHAR(255) NULL,
  PRIMARY KEY (id),
  KEY idx_grade_id (grade_id)
) ENGINE=InnoDB COMMENT='课程单元';

-- ------------------------------------------------------------
-- 6. 课时/章节
-- ------------------------------------------------------------
CREATE TABLE course_lesson (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  unit_id       BIGINT       NOT NULL,
  title         VARCHAR(100) NOT NULL COMMENT '课时标题',
  sort_order    INT          NOT NULL DEFAULT 0,
  content       TEXT         NULL COMMENT '课时介绍HTML',
  duration_min  INT          NOT NULL DEFAULT 40 COMMENT '建议学习时长(分钟)',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  PRIMARY KEY (id),
  KEY idx_unit_id (unit_id)
) ENGINE=InnoDB COMMENT='课时章节';

-- ------------------------------------------------------------
-- 7. 课时学习资源
-- ------------------------------------------------------------
CREATE TABLE course_resource (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  lesson_id     BIGINT       NOT NULL,
  title         VARCHAR(100) NOT NULL,
  res_type      VARCHAR(20)  NOT NULL COMMENT 'TEXT/DOC/LINK/WEB',
  content_url   VARCHAR(500) NULL COMMENT '外链或文件地址',
  content_text  TEXT         NULL COMMENT '图文内容',
  sort_order    INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_lesson_id (lesson_id)
) ENGINE=InnoDB COMMENT='课时资源';

-- ------------------------------------------------------------
-- 8. 互动学习任务
-- ------------------------------------------------------------
CREATE TABLE course_task (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  lesson_id     BIGINT       NOT NULL,
  title         VARCHAR(100) NOT NULL,
  description   TEXT         NULL,
  task_type     VARCHAR(20)  NOT NULL DEFAULT 'FORM' COMMENT 'FORM/CHOICE',
  config_json   TEXT         NULL COMMENT '题目配置JSON',
  max_score     INT          NOT NULL DEFAULT 10,
  sort_order    INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_lesson_id (lesson_id)
) ENGINE=InnoDB COMMENT='互动任务';

-- ------------------------------------------------------------
-- 9. 学生学习进度（课程地图状态）
-- ------------------------------------------------------------
CREATE TABLE learn_progress (
  id              BIGINT   NOT NULL AUTO_INCREMENT,
  student_id      BIGINT   NOT NULL,
  lesson_id       BIGINT   NOT NULL,
  status          VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED/IN_PROGRESS/COMPLETED',
  progress_percent INT     NOT NULL DEFAULT 0,
  study_seconds   INT      NOT NULL DEFAULT 0 COMMENT '累计学习秒数',
  last_visit_at   DATETIME NULL,
  completed_at    DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_lesson (student_id, lesson_id),
  KEY idx_lesson_id (lesson_id)
) ENGINE=InnoDB COMMENT='学习进度';

-- ------------------------------------------------------------
-- 10. 任务提交记录
-- ------------------------------------------------------------
CREATE TABLE learn_submission (
  id            BIGINT   NOT NULL AUTO_INCREMENT,
  task_id       BIGINT   NOT NULL,
  student_id    BIGINT   NOT NULL,
  content_json  TEXT     NOT NULL COMMENT '学生提交内容JSON',
  score         INT      NULL COMMENT '得分',
  status        VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
  study_seconds INT      NOT NULL DEFAULT 0,
  submitted_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_student (task_id, student_id),
  KEY idx_student_id (student_id)
) ENGINE=InnoDB COMMENT='任务提交';

-- ------------------------------------------------------------
-- 11. 访问记录
-- ------------------------------------------------------------
CREATE TABLE learn_visit_log (
  id            BIGINT   NOT NULL AUTO_INCREMENT,
  student_id    BIGINT   NOT NULL,
  lesson_id     BIGINT   NOT NULL,
  page_url      VARCHAR(255) NULL,
  duration_sec  INT      NOT NULL DEFAULT 0,
  visited_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_student_lesson (student_id, lesson_id)
) ENGINE=InnoDB COMMENT='访问日志';

-- ------------------------------------------------------------
-- 12. 积分记录
-- ------------------------------------------------------------
CREATE TABLE learn_points (
  id            BIGINT   NOT NULL AUTO_INCREMENT,
  student_id    BIGINT   NOT NULL,
  source_type   VARCHAR(20) NOT NULL COMMENT 'TASK/COMPLETE/BONUS',
  source_id     BIGINT   NULL,
  points        INT      NOT NULL,
  description   VARCHAR(200) NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_student_id (student_id)
) ENGINE=InnoDB COMMENT='积分记录';

-- ------------------------------------------------------------
-- 13. 系统配置
-- ------------------------------------------------------------
CREATE TABLE sys_config (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  config_key    VARCHAR(50)  NOT NULL,
  config_value  VARCHAR(500) NOT NULL,
  description   VARCHAR(200) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB COMMENT='系统配置';

-- ------------------------------------------------------------
-- 14. 教师发布活动记录（按班级）
-- ------------------------------------------------------------
CREATE TABLE course_activity_publish (
  id            BIGINT   NOT NULL AUTO_INCREMENT,
  lesson_id     BIGINT   NOT NULL,
  class_id      BIGINT   NOT NULL,
  resource_id   BIGINT   NULL,
  task_id       BIGINT   NULL,
  html_path     VARCHAR(500) NULL,
  published_by  BIGINT   NULL,
  published_at  DATETIME NULL,
  plan_json     TEXT     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_lesson_class (lesson_id, class_id)
) ENGINE=InnoDB COMMENT='课时活动发布记录';

-- ------------------------------------------------------------
-- 15. 学生活动提交明细日志
-- ------------------------------------------------------------
CREATE TABLE learn_submission_log (
  id              BIGINT   NOT NULL AUTO_INCREMENT,
  student_id      BIGINT   NOT NULL,
  lesson_id       BIGINT   NOT NULL,
  task_id         BIGINT   NOT NULL,
  content_json    TEXT     NOT NULL,
  score           INT      NULL,
  study_seconds   INT      NOT NULL DEFAULT 0,
  activity_key    VARCHAR(50) NULL,
  activity_index  INT      NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_student_lesson (student_id, lesson_id),
  KEY idx_student_created (student_id, created_at)
) ENGINE=InnoDB COMMENT='活动提交明细';

-- ------------------------------------------------------------
-- 14. 游学乐园访问申请与授权
-- ------------------------------------------------------------
CREATE TABLE learn_park_access (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  student_id    BIGINT       NOT NULL COMMENT '学生ID',
  class_id      BIGINT       NOT NULL COMMENT '班级ID',
  lesson_id     BIGINT       NULL COMMENT '申请时关联的当前课时',
  status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/REVOKED',
  applied_at    DATETIME     NULL,
  reviewed_at   DATETIME     NULL,
  reviewer_id   BIGINT       NULL COMMENT '审批教师',
  review_note   VARCHAR(200) NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_park_student (student_id),
  KEY idx_park_class_status (class_id, status)
) ENGINE=InnoDB COMMENT='游学乐园访问申请与授权';

-- ============================================================
-- 表关系说明：
-- course_grade 1:N course_unit 1:N course_lesson 1:N (course_resource, course_task)
-- sys_class N:1 sys_user(teacher)
-- sys_user(student) N:1 sys_class（也可通过 sys_class_student 关联）
-- learn_progress / learn_submission / learn_visit_log 均关联 student + lesson/task
-- ============================================================

-- ============================================================
-- 初始化演示数据（密码均为 123456，BCrypt加密）
-- 密码用 CONCAT(CHAR(36),...) 拼出 $，避免 Navicat 等工具把 $ 当变量解析导致整文件失败
-- ============================================================
INSERT INTO sys_user (username, password, real_name, role, class_id, status) VALUES ('admin', CONCAT(CHAR(36),'2b',CHAR(36),'10',CHAR(36),'W/PCZrjdz.jTDLRRklWBzumK7tDUmpuk/pseBDLVylqyvSfyb7HOK'), '系统管理员', 'ADMIN', NULL, 1), ('teacher1', CONCAT(CHAR(36),'2b',CHAR(36),'10',CHAR(36),'W/PCZrjdz.jTDLRRklWBzumK7tDUmpuk/pseBDLVylqyvSfyb7HOK'), '张老师', 'TEACHER', NULL, 1), ('student1', CONCAT(CHAR(36),'2b',CHAR(36),'10',CHAR(36),'W/PCZrjdz.jTDLRRklWBzumK7tDUmpuk/pseBDLVylqyvSfyb7HOK'), '李明', 'STUDENT', 1, 1), ('student2', CONCAT(CHAR(36),'2b',CHAR(36),'10',CHAR(36),'W/PCZrjdz.jTDLRRklWBzumK7tDUmpuk/pseBDLVylqyvSfyb7HOK'), '王芳', 'STUDENT', 1, 1), ('student3', CONCAT(CHAR(36),'2b',CHAR(36),'10',CHAR(36),'W/PCZrjdz.jTDLRRklWBzumK7tDUmpuk/pseBDLVylqyvSfyb7HOK'), '赵强', 'STUDENT', 1, 1);

INSERT INTO sys_class (name, grade_name, teacher_id) VALUES ('七年级1班', '七年级', 2);

UPDATE sys_user SET class_id = 1 WHERE username IN ('student1','student2','student3');

INSERT INTO sys_class_student (class_id, student_id) VALUES (1, 3), (1, 4), (1, 5);

-- 配套教材：三年级～六年级，上下册共 8 本；校本栏目占位 2 条（无封面、无单元，后续可维护）
INSERT INTO course_grade (name, sort_order, description, textbook_type, cover_url, pdf_url) VALUES
('三年级上册', 1, '义务教育教科书·信息科技（三年级上册）', 'MAIN', '/course-covers/grade3-up.jpg', '/textbooks/grade3-up.pdf'),
('三年级下册', 2, '义务教育教科书·信息科技（三年级下册）', 'MAIN', '/course-covers/grade3-down.jpg', '/textbooks/grade3-down.pdf'),
('四年级上册', 3, '义务教育教科书·信息科技（四年级上册）', 'MAIN', '/course-covers/grade4-up.jpg', '/textbooks/grade4-up.pdf'),
('四年级下册', 4, '义务教育教科书·信息科技（四年级下册）', 'MAIN', '/course-covers/grade4-down.jpg', '/textbooks/grade4-down.pdf'),
('五年级上册', 5, '义务教育教科书·信息科技（五年级上册）', 'MAIN', '/course-covers/grade5-up.jpg', '/textbooks/grade5-up.pdf'),
('五年级下册', 6, '义务教育教科书·信息科技（五年级下册）', 'MAIN', '/course-covers/grade5-down.jpg', '/textbooks/grade5-down.pdf'),
('六年级上册', 7, '义务教育教科书·信息科技（六年级上册·2026版）', 'MAIN', '/course-covers/grade6-up.jpg', '/textbooks/grade6-up.pdf'),
('六年级下册', 8, '义务教育教科书·信息科技（六年级下册）', 'MAIN', '/course-covers/grade6-down.jpg', '/textbooks/grade6-down.pdf'),
('二年级上册', 90, '科创启航 · 洞察未来科创启航（二年级上册）', 'SCHOOL', '/course-covers/kechuang-up.png', NULL),
('二年级下册', 91, '科创启航 · 洞察未来科创启航（二年级下册）', 'SCHOOL', '/course-covers/kechuang-down.png', NULL),
('二年级上册', 92, '娃娃讲科技 · 少儿趣味科普课堂（二年级上册）', 'SCHOOL', '/course-covers/wawa-tech-up.png', NULL),
('二年级下册', 93, '娃娃讲科技 · 少儿趣味科普课堂（二年级下册）', 'SCHOOL', '/course-covers/wawa-tech-down.png', NULL);

INSERT INTO course_unit (grade_id, name, sort_order, description) VALUES
((SELECT id FROM course_grade WHERE name='三年级上册' AND textbook_type='MAIN' LIMIT 1), '第一单元 感受信息社会', 1, NULL),
((SELECT id FROM course_grade WHERE name='三年级上册' AND textbook_type='MAIN' LIMIT 1), '第二单元 获取在线资源', 2, NULL),
((SELECT id FROM course_grade WHERE name='三年级上册' AND textbook_type='MAIN' LIMIT 1), '第三单元 体验在线生活', 3, NULL),
((SELECT id FROM course_grade WHERE name='三年级上册' AND textbook_type='MAIN' LIMIT 1), '第四单元 开展在线学习', 4, NULL),
((SELECT id FROM course_grade WHERE name='三年级下册' AND textbook_type='MAIN' LIMIT 1), '第一单元 整理数字资源', 1, NULL),
((SELECT id FROM course_grade WHERE name='三年级下册' AND textbook_type='MAIN' LIMIT 1), '第二单元 创作数字作品', 2, NULL),
((SELECT id FROM course_grade WHERE name='三年级下册' AND textbook_type='MAIN' LIMIT 1), '第三单元 在线学习小能手', 3, NULL),
((SELECT id FROM course_grade WHERE name='四年级上册' AND textbook_type='MAIN' LIMIT 1), '第一单元 泛在的数据', 1, NULL),
((SELECT id FROM course_grade WHERE name='四年级上册' AND textbook_type='MAIN' LIMIT 1), '第二单元 数据证明观点', 2, NULL),
((SELECT id FROM course_grade WHERE name='四年级上册' AND textbook_type='MAIN' LIMIT 1), '第三单元 身边的编码', 3, NULL),
((SELECT id FROM course_grade WHERE name='四年级下册' AND textbook_type='MAIN' LIMIT 1), '第一单元 数字世界', 1, NULL),
((SELECT id FROM course_grade WHERE name='四年级下册' AND textbook_type='MAIN' LIMIT 1), '第二单元 解码与校验', 2, NULL),
((SELECT id FROM course_grade WHERE name='四年级下册' AND textbook_type='MAIN' LIMIT 1), '第三单元 用数据讲故事', 3, NULL),
((SELECT id FROM course_grade WHERE name='五年级上册' AND textbook_type='MAIN' LIMIT 1), '第一单元 算法与算法表示', 1, NULL),
((SELECT id FROM course_grade WHERE name='五年级上册' AND textbook_type='MAIN' LIMIT 1), '第二单元 算法的控制结构', 2, NULL),
((SELECT id FROM course_grade WHERE name='五年级上册' AND textbook_type='MAIN' LIMIT 1), '第三单元 用算法解决问题', 3, NULL),
((SELECT id FROM course_grade WHERE name='五年级下册' AND textbook_type='MAIN' LIMIT 1), '第一单元 生活中的系统', 1, NULL),
((SELECT id FROM course_grade WHERE name='五年级下册' AND textbook_type='MAIN' LIMIT 1), '第二单元 控制系统', 2, NULL),
((SELECT id FROM course_grade WHERE name='五年级下册' AND textbook_type='MAIN' LIMIT 1), '第三单元 控制系统中的计算', 3, NULL),
((SELECT id FROM course_grade WHERE name='六年级上册' AND textbook_type='MAIN' LIMIT 1), '第一单元 算法的实现', 1, NULL),
((SELECT id FROM course_grade WHERE name='六年级上册' AND textbook_type='MAIN' LIMIT 1), '第二单元 算法的效率', 2, NULL),
((SELECT id FROM course_grade WHERE name='六年级上册' AND textbook_type='MAIN' LIMIT 1), '第三单元 算法的影响', 3, NULL),
((SELECT id FROM course_grade WHERE name='六年级下册' AND textbook_type='MAIN' LIMIT 1), '第一单元 控制系统中的反馈', 1, NULL),
((SELECT id FROM course_grade WHERE name='六年级下册' AND textbook_type='MAIN' LIMIT 1), '第二单元 控制系统中的运算', 2, NULL),
((SELECT id FROM course_grade WHERE name='六年级下册' AND textbook_type='MAIN' LIMIT 1), '第三单元 扩音系统', 3, NULL);

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
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第1课 算法与问题解决', 1, '<p>本课属于《六年级上册》（2026版）《第一单元 算法的实现》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第2课 抽象与建模', 2, '<p>本课属于《六年级上册》（2026版）《第一单元 算法的实现》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第3课 算法设计', 3, '<p>本课属于《六年级上册》（2026版）《第一单元 算法的实现》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第4课 用程序设计语言描述算法', 4, '<p>本课属于《六年级上册》（2026版）《第一单元 算法的实现》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第5课 算法执行', 5, '<p>本课属于《六年级上册》（2026版）《第一单元 算法的实现》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第6课 “猜数字”算法设计', 6, '<p>本课属于《六年级上册》（2026版）《第一单元 算法的实现》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第一单元 算法的实现' LIMIT 1), '第7课 用人工智能辅助算法实现', 7, '<p>本课属于《六年级上册》（2026版）《第一单元 算法的实现》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的效率' LIMIT 1), '第8课 算法的多样性', 1, '<p>本课属于《六年级上册》（2026版）《第二单元 算法的效率》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的效率' LIMIT 1), '第9课 算法的评价方法', 2, '<p>本课属于《六年级上册》（2026版）《第二单元 算法的效率》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的效率' LIMIT 1), '第10课 “韩信点兵”枚举法的实现', 3, '<p>本课属于《六年级上册》（2026版）《第二单元 算法的效率》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的效率' LIMIT 1), '第11课 “韩信点兵”筛选法的实现', 4, '<p>本课属于《六年级上册》（2026版）《第二单元 算法的效率》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第二单元 算法的效率' LIMIT 1), '第12课 用人工智能解决“韩信点兵”问题', 5, '<p>本课属于《六年级上册》（2026版）《第二单元 算法的效率》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 算法的影响' LIMIT 1), '第13课 在线生活中的算法', 1, '<p>本课属于《六年级上册》（2026版）《第三单元 算法的影响》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 算法的影响' LIMIT 1), '第14课 智能寻路中的搜索算法', 2, '<p>本课属于《六年级上册》（2026版）《第三单元 算法的影响》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 算法的影响' LIMIT 1), '第15课 在线购物中的推荐算法', 3, '<p>本课属于《六年级上册》（2026版）《第三单元 算法的影响》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 算法的影响' LIMIT 1), '第16课 算法对生活的影响', 4, '<p>本课属于《六年级上册》（2026版）《第三单元 算法的影响》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 算法的影响' LIMIT 1), '第17课 人机对话的实现', 5, '<p>本课属于《六年级上册》（2026版）《第三单元 算法的影响》。</p><p>请按教师课堂安排完成学习。</p>', 40),
((SELECT cu.id FROM course_unit cu INNER JOIN course_grade cg ON cu.grade_id = cg.id WHERE cg.name='六年级上册' AND cg.textbook_type='MAIN' AND cu.name='第三单元 算法的影响' LIMIT 1), '第18课 对话机器人', 6, '<p>本课属于《六年级上册》（2026版）《第三单元 算法的影响》。</p><p>请按教师课堂安排完成学习。</p>', 40),
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

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order) VALUES
(1, '课前思考', '想一想你每天会在哪些场景使用在线服务', 'FORM', '{"fields":[{"name":"content","label":"我的思考","type":"textarea","required":true}]}', 10, 1);

INSERT INTO sys_config (config_key, config_value, description) VALUES ('site_name', '中小学信息科技学习空间', '网站名称'), ('points_per_task', '5', '每完成一个任务奖励积分');

-- 五年级下册第8课：飞象探究单（试点）
INSERT INTO course_resource (lesson_id, title, res_type, content_url, content_text, sort_order)
SELECT cl.id, '体验控制系统探究单', 'WEB', '/lessons/g5-lesson8/index.html', NULL, 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '五年级下册' AND cu.name = '第二单元 控制系统' AND cl.title = '第8课 体验控制系统'
LIMIT 1;

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order)
SELECT cl.id, '探究单互动挑战', '飞象老师探究单：体验控制系统', 'EXTERNAL', '{"source":"feixiang","path":"/lessons/g5-lesson8/index.html"}', 100, 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '五年级下册' AND cu.name = '第二单元 控制系统' AND cl.title = '第8课 体验控制系统'
LIMIT 1;

-- 六年级上册第2课：抽象与建模学生工作台
INSERT INTO course_resource (lesson_id, title, res_type, content_url, sort_order)
SELECT cl.id, '抽象与建模探究活动', 'WEB', '/lessons/g6-up-lesson2/index.html', 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现' AND cl.title = '第2课 抽象与建模'
LIMIT 1;

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order)
SELECT cl.id,
  '抽象与建模探究',
  '六年级上第2课学生工作台',
  'EXTERNAL',
  '{"layout":"student_workspace","lessonTitle":"第2课 抽象与建模","objectives":"我能写出清晰的问题描述，用表格整理对象、数量与关系，并从表格列出方程完成抽象建模。","activities":[{"index":1,"title":"鸡兔同笼表格","step":1,"path":"/lessons/g6-up-lesson2/index.html","unlocked":true},{"index":2,"title":"百钱买百鸡建表","step":2,"path":"/lessons/g6-up-lesson2/index.html","unlocked":false},{"index":3,"title":"抽象建模挑战","step":3,"path":"/lessons/g6-up-lesson2/index.html","unlocked":false}],"quiz":{"title":"课堂小测","unlockAfterActivity":3,"questions":[]}}',
  100,
  1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现' AND cl.title = '第2课 抽象与建模'
LIMIT 1;

-- 六年级上册第3课：算法设计学生工作台（完整配置见 migration_g6_up_lesson3.sql）
INSERT INTO course_resource (lesson_id, title, res_type, content_url, sort_order)
SELECT cl.id, '算法设计探究活动', 'WEB', '/lessons/g6-up-lesson3/index.html', 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现' AND cl.title = '第3课 算法设计'
LIMIT 1;

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order)
SELECT cl.id,
  '算法设计探究',
  '六年级上第3课学生工作台',
  'EXTERNAL',
  '{"layout":"student_workspace","lessonTitle":"第3课 算法设计","objectives":"我能说出枚举法的思想，确定枚举范围与判断条件，并用流程图表示枚举算法。","activities":[{"index":1,"title":"手工枚举模拟器","step":1,"path":"/lessons/g6-up-lesson3/index.html","unlocked":true},{"index":2,"title":"流程图参数填空","step":2,"path":"/lessons/g6-up-lesson3/index.html","unlocked":false},{"index":3,"title":"百钱买百鸡双层枚举","step":3,"path":"/lessons/g6-up-lesson3/index.html","unlocked":false}],"quiz":{"title":"课堂小测","unlockAfterActivity":3,"questions":[]}}',
  100,
  1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现' AND cl.title = '第3课 算法设计'
LIMIT 1;

-- 六年级上册第4课：用程序设计语言描述算法学生工作台（完整配置见 migration_g6_up_lesson4.sql）
INSERT INTO course_resource (lesson_id, title, res_type, content_url, sort_order)
SELECT cl.id, '用程序设计语言描述算法探究活动', 'WEB', '/lessons/g6-up-lesson4/index.html', 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现' AND cl.title = '第4课 用程序设计语言描述算法'
LIMIT 1;

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order)
SELECT cl.id,
  '用程序设计语言描述算法探究',
  '六年级上第4课学生工作台',
  'EXTERNAL',
  '{"layout":"student_workspace","lessonTitle":"第4课 用程序设计语言描述算法","objectives":"我能把枚举法流程图逐框翻译成 Python 代码。","activities":[{"index":1,"title":"Python代码填空","step":1,"path":"/lessons/g6-up-lesson4/index.html","unlocked":true},{"index":2,"title":"改数字运行","step":2,"path":"/lessons/g6-up-lesson4/index.html","unlocked":false},{"index":3,"title":"嵌套循环填空","step":3,"path":"/lessons/g6-up-lesson4/index.html","unlocked":false}],"quiz":{"title":"课堂小测","unlockAfterActivity":3,"questions":[]}}',
  100,
  1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现' AND cl.title = '第4课 用程序设计语言描述算法'
LIMIT 1;

-- 六年级上册第5课：算法执行学生工作台（完整配置见 migration_g6_up_lesson5.sql）
INSERT INTO course_resource (lesson_id, title, res_type, content_url, sort_order)
SELECT cl.id, '算法执行探究活动', 'WEB', '/lessons/g6-up-lesson5/index.html', 1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现' AND cl.title = '第5课 算法执行'
LIMIT 1;

INSERT INTO course_task (lesson_id, title, description, task_type, config_json, max_score, sort_order)
SELECT cl.id,
  '算法执行探究',
  '六年级上第5课学生工作台',
  'EXTERNAL',
  '{"layout":"student_workspace","lessonTitle":"第5课 算法执行","objectives":"我能运行 Python 程序，读懂报错并调试，完善算法使程序更通用。","activities":[{"index":1,"title":"Bug猎人","step":1,"path":"/lessons/g6-up-lesson5/index.html","unlocked":true},{"index":2,"title":"完善算法","step":2,"path":"/lessons/g6-up-lesson5/index.html","unlocked":false},{"index":3,"title":"通用化改造","step":3,"path":"/lessons/g6-up-lesson5/index.html","unlocked":false}],"quiz":{"title":"课堂小测","unlockAfterActivity":3,"questions":[]}}',
  100,
  1
FROM course_lesson cl
INNER JOIN course_unit cu ON cl.unit_id = cu.id
INNER JOIN course_grade cg ON cu.grade_id = cg.id
WHERE cg.name = '六年级上册' AND cg.textbook_type = 'MAIN'
  AND cu.name = '第一单元 算法的实现' AND cl.title = '第5课 算法执行'
LIMIT 1;
