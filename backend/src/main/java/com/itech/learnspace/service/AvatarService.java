package com.itech.learnspace.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.entity.LearnPoints;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.LearnPointsRepository;
import com.itech.learnspace.repository.SysClassRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AvatarService {

    private static final List<Item> ITEMS = new ArrayList<Item>();

    static {
        add("boy-hair-short", "boy", "hair", "短发", 0, 1);
        add("boy-hair-sport", "boy", "hair", "呆毛", 8, 2);
        add("boy-hair-side", "boy", "hair", "侧分", 12, 3);
        add("boy-hair-wavy", "boy", "hair", "波浪发", 12, 21);
        add("boy-top-shirt", "boy", "outfit", "薄荷绿T恤", 0, 4);
        add("boy-top-jersey", "boy", "outfit", "球衣", 16, 5);
        add("boy-top-jacket", "boy", "outfit", "连帽外套", 20, 6);
        add("boy-top-sport", "boy", "outfit", "运动服", 18, 22);
        add("boy-top-tech", "boy", "outfit", "科技马甲", 20, 23);
        add("boy-extra-none", "boy", "extra", "无配饰", 0, 7);
        add("boy-extra-glasses", "boy", "extra", "眼镜", 10, 8);
        add("boy-extra-cap", "boy", "extra", "帽子", 12, 9);
        add("girl-hair-bob", "girl", "hair", "齐肩发", 0, 11);
        add("girl-hair-twin", "girl", "hair", "双马尾", 10, 12);
        add("girl-hair-long", "girl", "hair", "星星长发", 14, 13);
        add("girl-hair-bun", "girl", "hair", "丸子头", 12, 24);
        add("girl-top-dress", "girl", "outfit", "灰色T恤", 0, 14);
        add("girl-top-uniform", "girl", "outfit", "校服", 16, 15);
        add("girl-top-coat", "girl", "outfit", "紫色开衫", 20, 16);
        add("girl-top-sport", "girl", "outfit", "运动外套", 18, 25);
        add("girl-extra-none", "girl", "extra", "无配饰", 0, 17);
        add("girl-extra-bow", "girl", "extra", "蝴蝶结", 8, 18);
        add("girl-extra-glasses", "girl", "extra", "眼镜", 10, 19);
    }

    private final JdbcTemplate jdbcTemplate;
    private final LearnPointsRepository pointsRepository;
    private final SysClassRepository classRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AvatarService(JdbcTemplate jdbcTemplate,
                         LearnPointsRepository pointsRepository,
                         SysClassRepository classRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.pointsRepository = pointsRepository;
        this.classRepository = classRepository;
    }

    @PostConstruct
    public void ensureTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS learn_student_avatar ("
                + "student_id BIGINT NOT NULL,"
                + "figure VARCHAR(10) NULL,"
                + "owned_json VARCHAR(1000) NOT NULL,"
                + "equipped_json VARCHAR(500) NOT NULL,"
                + "updated_at DATETIME NULL,"
                + "PRIMARY KEY (student_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    public Map<String, Object> profile(SysUser student) {
        State state = load(student.getId());
        return view(student, state, remaining(student.getId()));
    }

    /** 教师查看学生个人中心，不保存形象，也不扣积分。 */
    public Map<String, Object> preview(SysUser teacher) {
        State state = defaultState();
        Map<String, Object> view = view(teacher, state, 15);
        view.put("preview", Boolean.TRUE);
        view.put("className", "教师查看");
        view.put("username", "—");
        view.put("realName", teacher.getRealName());
        return view;
    }

    @Transactional
    public Map<String, Object> chooseFigure(SysUser student, String figure) {
        if (!"boy".equals(figure) && !"girl".equals(figure)) {
            throw new BusinessException("请选择男生或女生形象");
        }
        State state = load(student.getId());
        state.figure = figure;
        save(student.getId(), state);
        return view(student, state, remaining(student.getId()));
    }

    @Transactional
    public Map<String, Object> equip(SysUser student, String itemId) {
        Item item = find(itemId);
        if (item == null) {
            throw new BusinessException("没有这件装扮");
        }
        State state = load(student.getId());
        if (state.figure == null) {
            throw new BusinessException("请先选择男生或女生形象");
        }
        if (!state.figure.equals(item.figure)) {
            throw new BusinessException("这件装扮和当前形象不匹配");
        }
        int balance = remaining(student.getId());
        boolean already = state.owned.contains(item.id);
        boolean paid = pointsRepository.existsByStudentIdAndSourceTypeAndSourceId(
                student.getId(), "AVATAR", (long) item.sourceId);
        boolean chargedNow = false;
        if (!already && item.cost > 0 && !paid) {
            if (balance < item.cost) {
                throw new BusinessException("还差 " + (item.cost - balance) + " 积分才能换上「" + item.name + "」");
            }
            LearnPoints bill = new LearnPoints();
            bill.setStudentId(student.getId());
            bill.setSourceType("AVATAR");
            bill.setSourceId((long) item.sourceId);
            bill.setPoints(-item.cost);
            bill.setDescription("装扮：" + item.name);
            pointsRepository.save(bill);
            chargedNow = true;
            state.owned.add(item.id);
            balance = remaining(student.getId());
        } else if (!already) {
            state.owned.add(item.id);
        }
        state.equipped.get(item.figure).put(item.slot, item.id);
        save(student.getId(), state);
        Map<String, Object> view = view(student, state, balance);
        view.put("message", chargedNow
                ? "用了 " + item.cost + " 积分，换上了「" + item.name + "」。"
                : "已经换上「" + item.name + "」。");
        return view;
    }

    private Map<String, Object> view(SysUser student, State state, int balance) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("className", className(student));
        m.put("username", student.getUsername());
        m.put("realName", student.getRealName());
        m.put("figure", state.figure);
        m.put("redeemablePoints", balance);
        m.put("equipped", state.figure == null ? defaultSlots("boy") : state.equipped.get(state.figure));
        m.put("owned", state.owned);
        List<Map<String, Object>> catalog = new ArrayList<Map<String, Object>>();
        for (Item item : ITEMS) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", item.id);
            row.put("figure", item.figure);
            row.put("slot", item.slot);
            row.put("name", item.name);
            row.put("cost", item.cost);
            row.put("owned", state.owned.contains(item.id));
            catalog.add(row);
        }
        m.put("catalog", catalog);
        return m;
    }

    private String className(SysUser student) {
        if (student.getClassId() == null) {
            return "";
        }
        return classRepository.findById(student.getClassId()).map(c -> c.getName()).orElse("");
    }

    private int remaining(Long studentId) {
        Integer n = pointsRepository.sumPointsByStudentId(studentId);
        return n == null ? 0 : n;
    }

    private State load(Long studentId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT figure, owned_json, equipped_json FROM learn_student_avatar WHERE student_id = ?",
                studentId);
        State state = defaultState();
        if (rows.isEmpty()) {
            return state;
        }
        Map<String, Object> row = rows.get(0);
        Object figure = row.get("figure");
        if (figure != null && ("boy".equals(figure.toString()) || "girl".equals(figure.toString()))) {
            state.figure = figure.toString();
        }
        try {
            List<String> owned = objectMapper.readValue(String.valueOf(row.get("owned_json")),
                    new TypeReference<List<String>>() {});
            if (owned != null) {
                state.owned = owned;
            }
            Map<String, Map<String, String>> equipped = objectMapper.readValue(
                    String.valueOf(row.get("equipped_json")),
                    new TypeReference<Map<String, Map<String, String>>>() {});
            if (equipped != null) {
                if (equipped.get("boy") != null) {
                    state.equipped.put("boy", mergeSlots("boy", equipped.get("boy")));
                }
                if (equipped.get("girl") != null) {
                    state.equipped.put("girl", mergeSlots("girl", equipped.get("girl")));
                }
            }
        } catch (Exception ignored) {
            return defaultState();
        }
        for (Item item : ITEMS) {
            if (item.cost == 0 && !state.owned.contains(item.id)) {
                state.owned.add(item.id);
            }
        }
        return state;
    }

    private void save(Long studentId, State state) {
        try {
            String owned = objectMapper.writeValueAsString(state.owned);
            String equipped = objectMapper.writeValueAsString(state.equipped);
            jdbcTemplate.update(
                    "INSERT INTO learn_student_avatar (student_id, figure, owned_json, equipped_json, updated_at) "
                            + "VALUES (?, ?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE "
                            + "figure = VALUES(figure), owned_json = VALUES(owned_json), "
                            + "equipped_json = VALUES(equipped_json), updated_at = NOW()",
                    studentId, state.figure, owned, equipped);
        } catch (Exception e) {
            throw new BusinessException("形象保存失败");
        }
    }

    private State defaultState() {
        State state = new State();
        state.equipped.put("boy", defaultSlots("boy"));
        state.equipped.put("girl", defaultSlots("girl"));
        for (Item item : ITEMS) {
            if (item.cost == 0) {
                state.owned.add(item.id);
            }
        }
        return state;
    }

    private Map<String, String> defaultSlots(String figure) {
        Map<String, String> slots = new LinkedHashMap<String, String>();
        slots.put("hair", "boy".equals(figure) ? "boy-hair-short" : "girl-hair-bob");
        slots.put("outfit", "boy".equals(figure) ? "boy-top-shirt" : "girl-top-dress");
        slots.put("extra", "boy".equals(figure) ? "boy-extra-none" : "girl-extra-none");
        return slots;
    }

    private Map<String, String> mergeSlots(String figure, Map<String, String> saved) {
        Map<String, String> slots = defaultSlots(figure);
        for (String key : slots.keySet()) {
            String value = saved.get(key);
            if (find(value) != null && figure.equals(find(value).figure) && key.equals(find(value).slot)) {
                slots.put(key, value);
            }
        }
        return slots;
    }

    private Item find(String id) {
        if (id == null) {
            return null;
        }
        for (Item item : ITEMS) {
            if (item.id.equals(id)) {
                return item;
            }
        }
        return null;
    }

    private static void add(String id, String figure, String slot, String name, int cost, int sourceId) {
        Item item = new Item();
        item.id = id;
        item.figure = figure;
        item.slot = slot;
        item.name = name;
        item.cost = cost;
        item.sourceId = sourceId;
        ITEMS.add(item);
    }

    private static class Item {
        String id;
        String figure;
        String slot;
        String name;
        int cost;
        int sourceId;
    }

    private static class State {
        String figure;
        List<String> owned = new ArrayList<String>();
        Map<String, Map<String, String>> equipped = new LinkedHashMap<String, Map<String, String>>();
    }
}
