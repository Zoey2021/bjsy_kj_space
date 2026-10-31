-- 游学乐园全校参数：开放哪些游戏、接水果生命与速度、记忆翻翻乐对数
CREATE TABLE IF NOT EXISTS learn_park_setting (
  id INT NOT NULL,
  config_json TEXT NOT NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
