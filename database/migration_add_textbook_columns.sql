-- 仅当 course_grade 尚无 textbook_type 列时执行（若报错 Duplicate column 说明已升级，可忽略）
USE learn_space;
ALTER TABLE course_grade
  ADD COLUMN textbook_type VARCHAR(20) NOT NULL DEFAULT 'MAIN' COMMENT 'MAIN配套 SCHOOL校本' AFTER description,
  ADD COLUMN cover_url VARCHAR(500) NULL COMMENT '封面路径' AFTER textbook_type;
