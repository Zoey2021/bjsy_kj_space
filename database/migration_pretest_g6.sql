-- 六年级信息科技前测（2021 级）
SET NAMES utf8mb4;
USE learn_space;

CREATE TABLE IF NOT EXISTS learn_pretest_g6 (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  student_id     BIGINT       NOT NULL,
  class_id       BIGINT       NULL,
  student_name   VARCHAR(50)  NOT NULL,
  class_name     VARCHAR(50)  NULL,
  student_no     VARCHAR(20)  NULL,
  fill_score     INT          NOT NULL DEFAULT 0,
  choice_right   INT          NOT NULL DEFAULT 0,
  choice_score   INT          NOT NULL DEFAULT 0,
  op_score       INT          NOT NULL DEFAULT 0,
  att_score      INT          NOT NULL DEFAULT 0,
  total_score    INT          NOT NULL DEFAULT 0,
  level_code     VARCHAR(4)   NULL COMMENT 'A/B/C',
  content_json   LONGTEXT     NOT NULL,
  submitted_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pretest_g6_student (student_id),
  KEY idx_pretest_g6_class (class_id)
) ENGINE=InnoDB COMMENT='六年级信息科技前测提交';
