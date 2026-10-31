-- 门户身份：信息老师登录后进入现有教师工作台。可重复执行。
ALTER TABLE sys_user ADD COLUMN identity_code VARCHAR(32) NULL;

UPDATE sys_user
SET identity_code = 'INFO_TEACHER'
WHERE real_name IN ('张老师', '贾斌', '倪婕妤');
