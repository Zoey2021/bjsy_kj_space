-- 修复脚手架模板乱码（迁移时未指定 utf8mb4 导致）
SET NAMES utf8mb4;

UPDATE scaffold_templates
SET
  name = '六年级算法单元通用模板',
  tier_a_json = '{"goal":"不仅完成基础任务，还能优化、改造算法，解决开放性变式问题","criteria":["能独立完成基础算法任务","能提出优化或改造思路","能解决开放性变式问题"]}',
  tier_b_json = '{"goal":"全部完成课标基础任务；模仿迁移解决同类简单问题（课堂达成基准）","criteria":["能按步骤完成基础任务","能模仿示例解决同类问题","提示使用后能独立完成"]}',
  tier_c_json = '{"goal":"能看懂别人的算法实例，会识别顺序/分支/循环三种基本结构，完成观察、识别即可","criteria":["能看懂完整示例","能识别顺序、分支、循环","能完成 tracing 观察题"]}'
WHERE name NOT LIKE '%六年级%'
   OR name LIKE '%æ%'
   OR name LIKE '%ç%'
   OR name LIKE '%å%';
