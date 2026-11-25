package com.itech.learnspace.config;

import com.itech.learnspace.entity.LessonScaffold;
import com.itech.learnspace.repository.LessonScaffoldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 六年级上第5～8课：写入并发布 ABC 分层支架（与第1～4课同一套 lesson_scaffolds）。
 */
@Component
@Order(32)
public class G6UpLesson567ScaffoldSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(G6UpLesson567ScaffoldSeeder.class);

    private static final Map<String, LessonSpec> SPECS = new LinkedHashMap<String, LessonSpec>();
    static {
        SPECS.put("5", new LessonSpec(
                "第5课 算法执行",
                "第一单元 算法的实现",
                new String[]{"第5课 算法执行", "第5课 算法的执行"},
                "seed/g6-up-lesson5-scaffold.json",
                "Bug修复"));
        SPECS.put("6", new LessonSpec(
                "第6课 “猜数字”算法设计",
                "第一单元 算法的实现",
                new String[]{"第6课 “猜数字”算法设计", "第6课 猜数字算法设计"},
                "seed/g6-up-lesson6-scaffold.json",
                "抽象建模"));
        SPECS.put("7", new LessonSpec(
                "第7课 用人工智能辅助算法实现",
                "第一单元 算法的实现",
                new String[]{"第7课 用人工智能辅助算法实现", "第7课 猜数字算法验证"},
                "seed/g6-up-lesson7-scaffold.json",
                "算法小助手辅助实现"));
        SPECS.put("8", new LessonSpec(
                "第8课 算法的多样性",
                "第二单元 算法的效率",
                new String[]{"第8课 算法的多样性"},
                "seed/g6-up-lesson8-scaffold.json",
                "折半涂色卡"));
    }

    private final JdbcTemplate jdbcTemplate;
    private final LessonScaffoldRepository scaffoldRepository;

    public G6UpLesson567ScaffoldSeeder(JdbcTemplate jdbcTemplate,
                                       LessonScaffoldRepository scaffoldRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.scaffoldRepository = scaffoldRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Map.Entry<String, LessonSpec> e : SPECS.entrySet()) {
            seedOne(e.getKey(), e.getValue());
        }
    }

    private void seedOne(String no, LessonSpec spec) {
        Long lessonId = findLessonId(spec.unitName, spec.titles);
        if (lessonId == null) {
            log.warn("未找到六年级上第{}课，跳过 ABC 支架", no);
            return;
        }
        String itemsJson = readText(spec.resource);
        if (!StringUtils.hasText(itemsJson)) {
            log.warn("读取第{}课支架失败：{}", no, spec.resource);
            return;
        }
        LessonScaffold row = scaffoldRepository.findByLessonId(lessonId).orElse(null);
        if (row != null && "published".equals(row.getStatus())
                && StringUtils.hasText(row.getItemsJson())
                && row.getItemsJson().contains(spec.marker)) {
            return;
        }
        boolean created = row == null;
        if (row == null) {
            row = new LessonScaffold();
            row.setLessonId(lessonId);
        }
        row.setItemsJson(itemsJson);
        row.setStatus("published");
        scaffoldRepository.save(row);
        log.info("{}六年级上第{}课 ABC 支架并已发布：lessonId={}",
                created ? "已写入" : "已更新", no, lessonId);
    }

    private Long findLessonId(String unitName, String[] titles) {
        String sql = "SELECT cl.id FROM course_lesson cl "
                + "INNER JOIN course_unit cu ON cl.unit_id = cu.id "
                + "INNER JOIN course_grade cg ON cu.grade_id = cg.id "
                + "WHERE cg.name = ? AND cg.textbook_type = ? AND cu.name = ? AND cl.title = ? LIMIT 1";
        for (String title : titles) {
            List<Long> ids = jdbcTemplate.query(sql,
                    (rs, rowNum) -> rs.getLong(1),
                    "六年级上册", "MAIN", unitName, title);
            if (!ids.isEmpty()) {
                return ids.get(0);
            }
        }
        return null;
    }

    private String readText(String path) {
        try (java.io.InputStream in = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            log.warn("读取 {} 失败: {}", path, e.getMessage());
            return null;
        }
    }

    private static final class LessonSpec {
        private final String label;
        private final String unitName;
        private final String[] titles;
        private final String resource;
        private final String marker;

        private LessonSpec(String label, String unitName, String[] titles, String resource, String marker) {
            this.label = label;
            this.unitName = unitName;
            this.titles = titles;
            this.resource = resource;
            this.marker = marker;
        }
    }
}
