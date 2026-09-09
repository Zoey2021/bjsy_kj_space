-- 四年级信息科技基础水平前测（2023 级）
SET NAMES utf8mb4;
USE learn_space;

CREATE TABLE IF NOT EXISTS learn_pretest_g4 (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  student_id     BIGINT       NOT NULL COMMENT '学生ID',
  class_id       BIGINT       NULL COMMENT '班级ID',
  student_name   VARCHAR(50)  NOT NULL COMMENT '提交时姓名',
  class_name     VARCHAR(50)  NULL COMMENT '提交时班级名',
  student_no     VARCHAR(20)  NULL COMMENT '学号（从账号解析）',
  fill_score     INT          NOT NULL DEFAULT 0 COMMENT '填空机判分（每空2分，共20）',
  choice_right   INT          NOT NULL DEFAULT 0 COMMENT '选择题答对题数',
  choice_score   INT          NOT NULL DEFAULT 0 COMMENT '选择机判分（每题3分，共30）',
  op_score       INT          NOT NULL DEFAULT 0 COMMENT '操作题机判分（共40）',
  total_score    INT          NOT NULL DEFAULT 0 COMMENT '机判总分（满分90，填空/选择主观由教师复核）',
  content_json   LONGTEXT     NOT NULL COMMENT '完整作答与虚拟机状态',
  submitted_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pretest_g4_student (student_id),
  KEY idx_pretest_g4_class (class_id)
) ENGINE=InnoDB COMMENT='四年级信息科技基础水平前测提交';
