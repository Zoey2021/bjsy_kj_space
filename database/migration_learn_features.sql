-- 学生学习增强：当前课时、通知、班级当前课标记

ALTER TABLE sys_class
  ADD COLUMN current_lesson_id BIGINT NULL COMMENT '班级当前进行中的课时' AFTER login_code_expires_at;

CREATE TABLE IF NOT EXISTS learn_notification (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  class_id          BIGINT       NULL,
  target_student_id BIGINT       NULL COMMENT 'NULL 表示全班广播',
  sender_id         BIGINT       NOT NULL,
  msg_type          VARCHAR(32)  NOT NULL COMMENT 'remind/guide/broadcast',
  message           TEXT         NULL,
  payload_json      TEXT         NULL,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at           DATETIME     NULL,
  PRIMARY KEY (id),
  KEY idx_notify_student (target_student_id, created_at),
  KEY idx_notify_class (class_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师干预/学生通知';
