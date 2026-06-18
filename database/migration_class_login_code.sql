-- 班级码登录：教师选班后生成 6 位数字，学生凭码选名登录
USE learn_space;

ALTER TABLE sys_class
  ADD COLUMN login_code VARCHAR(6) NULL COMMENT '班级登录码' AFTER teacher_id,
  ADD COLUMN login_code_expires_at DATETIME NULL COMMENT '班级码过期时间' AFTER login_code,
  ADD UNIQUE KEY uk_login_code (login_code);
