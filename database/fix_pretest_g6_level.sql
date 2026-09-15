-- 六年级前测：按总分重算档位。A 仍保留「≥85 且原先已是 A」；65～84 一律 B；低于 65 为 C。
UPDATE learn_pretest_g6
SET level_code = CASE
  WHEN total_score < 65 THEN 'C'
  WHEN total_score >= 85 AND level_code = 'A' THEN 'A'
  ELSE 'B'
END
WHERE total_score IS NOT NULL;
