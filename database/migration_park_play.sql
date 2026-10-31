-- 游学乐园：学生游玩记录（游戏名称、人数、时长）

CREATE TABLE IF NOT EXISTS learn_park_play (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  student_id        BIGINT       NOT NULL,
  class_id          BIGINT       NOT NULL,
  game_id           VARCHAR(32)  NOT NULL,
  game_title        VARCHAR(50)  NOT NULL,
  duration_seconds  INT          NOT NULL,
  played_at         DATETIME     NOT NULL,
  PRIMARY KEY (id),
  KEY idx_park_play_class (class_id, played_at),
  KEY idx_park_play_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游学乐园游玩记录';
