-- 课堂积分商城 + 环节积分规则
CREATE TABLE IF NOT EXISTS learn_mall_item (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  code       VARCHAR(40)  NOT NULL,
  name       VARCHAR(80)  NOT NULL,
  icon       VARCHAR(20)  NOT NULL,
  cost       INT          NOT NULL,
  sort_order INT          NOT NULL DEFAULT 0,
  enabled    INT          NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mall_item_code (code)
) ENGINE=InnoDB COMMENT='积分商城奖品';

CREATE TABLE IF NOT EXISTS learn_mall_order (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  student_id BIGINT       NOT NULL,
  class_id   BIGINT       NULL,
  item_id    BIGINT       NOT NULL,
  item_name  VARCHAR(80)  NOT NULL,
  item_icon  VARCHAR(20)  NULL,
  points     INT          NOT NULL,
  status     VARCHAR(20)  NOT NULL DEFAULT 'APPLIED',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_mall_order_student (student_id),
  KEY idx_mall_order_class (class_id)
) ENGINE=InnoDB COMMENT='积分兑换申请';

CREATE TABLE IF NOT EXISTS learn_mall_class (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  class_id   BIGINT   NOT NULL,
  apply_open INT      NOT NULL DEFAULT 0,
  opened_by  BIGINT   NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mall_class (class_id)
) ENGINE=InnoDB COMMENT='班级是否开放兑换申请';

INSERT INTO learn_mall_item (code, name, icon, cost, sort_order, enabled)
SELECT * FROM (
  SELECT 'star_future_2' AS code, '星未来积分2' AS name, '🌟' AS icon, 10 AS cost, 1 AS sort_order, 1 AS enabled
  UNION ALL SELECT 'snack', '零食1份', '🍪', 20, 2, 1
  UNION ALL SELECT 'stationery', '文具1份', '✏️', 20, 3, 1
  UNION ALL SELECT 'praise_letter', '表扬信', '💌', 20, 4, 1
  UNION ALL SELECT 'milk_tea', '奶茶1份', '🧋', 50, 5, 1
  UNION ALL SELECT 'info_blindbox', '信息盲盒小礼物', '🎁', 20, 6, 1
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM learn_mall_item LIMIT 1);

INSERT INTO sys_config (config_key, config_value, description)
SELECT 'points_per_activity', '2', '每完成一个探究环节奖励积分'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'points_per_activity');

INSERT INTO sys_config (config_key, config_value, description)
SELECT 'points_extension_bonus', '1', '完整完成拓展任务额外积分'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'points_extension_bonus');
