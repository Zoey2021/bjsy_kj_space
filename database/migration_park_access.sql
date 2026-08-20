-- 游学乐园：学生申请 + 教师授权

CREATE TABLE IF NOT EXISTS learn_park_access (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游学乐园访问申请与授权';
