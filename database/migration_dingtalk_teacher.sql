-- 已有库增加钉钉教师绑定字段。后端启动时也会自动补这一列。
ALTER TABLE sys_user
  ADD COLUMN dingtalk_union_id VARCHAR(64) NULL COMMENT '钉钉用户unionId，仅本校教师绑定';

CREATE UNIQUE INDEX uk_sys_user_dingtalk_union ON sys_user (dingtalk_union_id);
