-- 恢复教师/管理员登录账号（密码均为 123456）
-- 在宝塔终端执行，或导入到 phpMyAdmin / Navicat
SET NAMES utf8mb4;
USE learn_space;

-- 与系统原演示账号相同的 BCrypt（明文 123456）
SET @pwd = CONCAT(CHAR(36),'2b',CHAR(36),'10',CHAR(36),'W/PCZrjdz.jTDLRRklWBzumK7tDUmpuk/pseBDLVylqyvSfyb7HOK');

INSERT INTO sys_user (username, password, real_name, role, class_id, status)
VALUES ('18857121982', @pwd, '张老师', 'TEACHER', NULL, 1)
ON DUPLICATE KEY UPDATE
  password = @pwd,
  real_name = '张老师',
  role = 'TEACHER',
  class_id = NULL,
  status = 1;

INSERT INTO sys_user (username, password, real_name, role, class_id, status)
VALUES ('admin', @pwd, '系统管理员', 'ADMIN', NULL, 1)
ON DUPLICATE KEY UPDATE
  password = @pwd,
  real_name = '系统管理员',
  role = 'ADMIN',
  class_id = NULL,
  status = 1;

INSERT INTO sys_user (username, password, real_name, role, class_id, status)
VALUES ('teacher1', @pwd, '张老师', 'TEACHER', NULL, 1)
ON DUPLICATE KEY UPDATE
  password = @pwd,
  real_name = '张老师',
  role = 'TEACHER',
  class_id = NULL,
  status = 1;

-- 班级原先挂在已删除教师上时，改挂到新教师，否则「班级学情」会是空的
UPDATE sys_class c
LEFT JOIN sys_user u ON u.id = c.teacher_id
SET c.teacher_id = (SELECT id FROM sys_user WHERE username = '18857121982' LIMIT 1)
WHERE u.id IS NULL;

SELECT id, username, real_name, role, status
FROM sys_user
WHERE username IN ('18857121982', 'admin', 'teacher1');
