-- 分层脚手架工作台
SET NAMES utf8mb4;
SET @db := DATABASE();
SET @exist := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @db AND table_name = 'sys_user' AND column_name = 'tier_override'
);
SET @sql := IF(@exist = 0,
  'ALTER TABLE sys_user ADD COLUMN tier_override VARCHAR(4) NULL COMMENT ''教师手动覆盖档位 A/B/C'' AFTER class_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS scaffold_templates (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  name       VARCHAR(100) NOT NULL,
  tier_a_json TEXT        NOT NULL,
  tier_b_json TEXT        NOT NULL,
  tier_c_json TEXT        NOT NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='脚手架档位模板';

CREATE TABLE IF NOT EXISTS lesson_scaffolds (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  lesson_id   BIGINT       NOT NULL,
  template_id BIGINT       NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'draft',
  items_json  LONGTEXT     NOT NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_lesson_scaffold (lesson_id)
) ENGINE=InnoDB COMMENT='课时脚手架';

CREATE TABLE IF NOT EXISTS learn_hint_log (
  id             BIGINT   NOT NULL AUTO_INCREMENT,
  student_id     BIGINT   NOT NULL,
  lesson_id      BIGINT   NOT NULL,
  activity_index INT      NOT NULL,
  hint_index     INT      NOT NULL,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_hint_once (student_id, lesson_id, activity_index, hint_index),
  KEY idx_hint_lesson (lesson_id, activity_index)
) ENGINE=InnoDB COMMENT='学生提示使用记录';

INSERT INTO scaffold_templates (name, tier_a_json, tier_b_json, tier_c_json)
SELECT * FROM (
  SELECT
    '六年级算法单元通用模板' AS name,
    '{"goal":"不仅完成基础任务，还能优化、改造算法，解决开放性变式问题","criteria":["能独立完成基础算法任务","能提出优化或改造思路","能解决开放性变式问题"]}' AS tier_a_json,
    '{"goal":"全部完成课标基础任务；模仿迁移解决同类简单问题（课堂达成基准）","criteria":["能按步骤完成基础任务","能模仿示例解决同类问题","提示使用后能独立完成"]}' AS tier_b_json,
    '{"goal":"能看懂别人的算法实例，会识别顺序/分支/循环三种基本结构，完成观察、识别即可","criteria":["能看懂完整示例","能识别顺序、分支、循环","能完成 tracing 观察题"]}' AS tier_c_json
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM scaffold_templates LIMIT 1);
