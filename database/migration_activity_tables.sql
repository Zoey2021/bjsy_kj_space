-- 已有数据库执行本脚本，补充活动相关表
USE learn_space;

CREATE TABLE IF NOT EXISTS course_activity_publish (
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

CREATE TABLE IF NOT EXISTS learn_submission_log (
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
