package com.itech.learnspace.service;

import com.itech.learnspace.dto.MallOpenRequest;
import com.itech.learnspace.dto.MallRedeemRequest;
import com.itech.learnspace.dto.PointsRulesRequest;
import com.itech.learnspace.entity.LearnPoints;
import com.itech.learnspace.entity.PointsMallClassOpen;
import com.itech.learnspace.entity.PointsMallItem;
import com.itech.learnspace.entity.PointsMallOrder;
import com.itech.learnspace.entity.SysClass;
import com.itech.learnspace.entity.SysConfig;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.LearnPointsRepository;
import com.itech.learnspace.repository.PointsMallClassOpenRepository;
import com.itech.learnspace.repository.PointsMallItemRepository;
import com.itech.learnspace.repository.PointsMallOrderRepository;
import com.itech.learnspace.repository.SysClassRepository;
import com.itech.learnspace.repository.SysConfigRepository;
import com.itech.learnspace.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PointsMallService {

    public static final String KEY_ACTIVITY = "points_per_activity";
    public static final String KEY_EXTENSION = "points_extension_bonus";
    public static final int DEFAULT_ACTIVITY = 2;
    public static final int DEFAULT_EXTENSION = 1;

    private final PointsMallItemRepository itemRepository;
    private final PointsMallOrderRepository orderRepository;
    private final PointsMallClassOpenRepository classOpenRepository;
    private final LearnPointsRepository pointsRepository;
    private final SysConfigRepository configRepository;
    private final SysClassRepository classRepository;
    private final SysUserRepository userRepository;
    private final DashboardService dashboardService;

    public PointsMallService(PointsMallItemRepository itemRepository,
                             PointsMallOrderRepository orderRepository,
                             PointsMallClassOpenRepository classOpenRepository,
                             LearnPointsRepository pointsRepository,
                             SysConfigRepository configRepository,
                             SysClassRepository classRepository,
                             SysUserRepository userRepository,
                             DashboardService dashboardService) {
        this.itemRepository = itemRepository;
        this.orderRepository = orderRepository;
        this.classOpenRepository = classOpenRepository;
        this.pointsRepository = pointsRepository;
        this.configRepository = configRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.dashboardService = dashboardService;
    }

    public Map<String, Object> getRules() {
        ensureDefaultItems();
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("pointsPerActivity", readInt(KEY_ACTIVITY, DEFAULT_ACTIVITY));
        m.put("extensionBonus", readInt(KEY_EXTENSION, DEFAULT_EXTENSION));
        return m;
    }

    @Transactional
    public Map<String, Object> saveRules(PointsRulesRequest req) {
        int activity = req.getPointsPerActivity() == null ? DEFAULT_ACTIVITY : req.getPointsPerActivity();
        int bonus = req.getExtensionBonus() == null ? DEFAULT_EXTENSION : req.getExtensionBonus();
        if (activity < 0 || activity > 20) {
            throw new BusinessException("每个环节积分需在 0～20 之间");
        }
        if (bonus < 0 || bonus > 10) {
            throw new BusinessException("拓展任务额外积分需在 0～10 之间");
        }
        upsertConfig(KEY_ACTIVITY, String.valueOf(activity), "每完成一个探究环节奖励积分");
        upsertConfig(KEY_EXTENSION, String.valueOf(bonus), "完整完成拓展任务额外积分");
        return getRules();
    }

    public Map<String, Object> teacherMall(Long classId) {
        ensureDefaultItems();
        if (classId != null) {
            classRepository.findById(classId).orElseThrow(() -> new BusinessException("班级不存在"));
        }
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("items", toItemMaps(itemRepository.findByEnabledOrderBySortOrderAscIdAsc(1)));
        m.put("applyOpen", classId != null && isApplyOpen(classId));
        m.put("classId", classId);
        if (classId != null) {
            classRepository.findById(classId).ifPresent(c -> m.put("className", c.getName()));
            m.put("orders", toOrderMaps(orderRepository.findByClassIdOrderByCreatedAtDesc(classId)));
        } else {
            m.put("orders", new ArrayList<Map<String, Object>>());
        }
        m.putAll(getRules());
        return m;
    }

    @Transactional
    public Map<String, Object> setApplyOpen(Long teacherId, String role, MallOpenRequest req) {
        if (req == null || req.getClassId() == null) {
            throw new BusinessException("请选择班级");
        }
        if (!"ADMIN".equals(role)) {
            dashboardService.checkTeacherOwnsClass(teacherId, req.getClassId());
        }
        SysClass cls = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new BusinessException("班级不存在"));
        boolean open = Boolean.TRUE.equals(req.getOpen());
        PointsMallClassOpen row = classOpenRepository.findByClassId(cls.getId()).orElseGet(PointsMallClassOpen::new);
        row.setClassId(cls.getId());
        row.setApplyOpen(open ? 1 : 0);
        row.setOpenedBy(teacherId);
        classOpenRepository.save(row);
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("classId", cls.getId());
        m.put("className", cls.getName());
        m.put("applyOpen", open);
        return m;
    }

    public Map<String, Object> studentMall(SysUser student) {
        ensureDefaultItems();
        if (student.getClassId() == null) {
            throw new BusinessException("当前账号未绑定班级");
        }
        int remaining = remainingPoints(student.getId());
        int earned = earnedPoints(student.getId());
        boolean open = isApplyOpen(student.getClassId());
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (PointsMallItem item : itemRepository.findByEnabledOrderBySortOrderAscIdAsc(1)) {
            Map<String, Object> map = toItemMap(item);
            map.put("canApply", open && remaining >= item.getCost());
            items.add(map);
        }
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("items", items);
        m.put("applyOpen", open);
        m.put("redeemablePoints", remaining);
        m.put("earnedPoints", earned);
        m.put("orders", toOrderMaps(orderRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())));
        return m;
    }

    @Transactional
    public Map<String, Object> redeem(SysUser student, MallRedeemRequest req) {
        if (student.getClassId() == null) {
            throw new BusinessException("当前账号未绑定班级");
        }
        if (req == null || req.getItemId() == null) {
            throw new BusinessException("请选择兑换奖品");
        }
        if (!isApplyOpen(student.getClassId())) {
            throw new BusinessException("教师尚未开放兑换申请");
        }
        PointsMallItem item = itemRepository.findById(req.getItemId())
                .orElseThrow(() -> new BusinessException("奖品不存在"));
        if (item.getEnabled() == null || item.getEnabled() != 1) {
            throw new BusinessException("该奖品暂不可兑换");
        }
        int remaining = remainingPoints(student.getId());
        if (remaining < item.getCost()) {
            throw new BusinessException("积分不足，还差 " + (item.getCost() - remaining) + " 分");
        }

        PointsMallOrder order = new PointsMallOrder();
        order.setStudentId(student.getId());
        order.setClassId(student.getClassId());
        order.setItemId(item.getId());
        order.setItemName(item.getName());
        order.setItemIcon(item.getIcon());
        order.setPoints(item.getCost());
        order.setStatus("APPLIED");
        order = orderRepository.save(order);

        LearnPoints record = new LearnPoints();
        record.setStudentId(student.getId());
        record.setSourceType("REDEEM");
        record.setSourceId(order.getId());
        record.setPoints(-item.getCost());
        record.setDescription("兑换：" + item.getName());
        pointsRepository.save(record);

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("order", toOrderMap(order, student.getRealName()));
        m.put("redeemablePoints", remainingPoints(student.getId()));
        m.put("earnedPoints", earnedPoints(student.getId()));
        return m;
    }

    public int remainingPoints(Long studentId) {
        Integer n = pointsRepository.sumPointsByStudentId(studentId);
        return n == null ? 0 : n;
    }

    public int earnedPoints(Long studentId) {
        Integer n = pointsRepository.sumEarnedByStudentId(studentId);
        return n == null ? 0 : n;
    }

    public boolean isApplyOpen(Long classId) {
        return classOpenRepository.findByClassId(classId)
                .map(row -> row.getApplyOpen() != null && row.getApplyOpen() == 1)
                .orElse(false);
    }

    public int readInt(String key, int fallback) {
        SysConfig config = configRepository.findByConfigKey(key).orElse(null);
        if (config == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void upsertConfig(String key, String value, String desc) {
        SysConfig config = configRepository.findByConfigKey(key).orElseGet(SysConfig::new);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription(desc);
        configRepository.save(config);
    }

    private void ensureDefaultItems() {
        if (itemRepository.count() > 0) {
            return;
        }
        seedItem("star_future_2", "星未来积分2", "🌟", 10, 1);
        seedItem("snack", "零食1份", "🍪", 20, 2);
        seedItem("stationery", "文具1份", "✏️", 20, 3);
        seedItem("praise_letter", "表扬信", "💌", 20, 4);
        seedItem("milk_tea", "奶茶1份", "🧋", 50, 5);
        seedItem("info_blindbox", "信息盲盒小礼物", "🎁", 20, 6);
    }

    private void seedItem(String code, String name, String icon, int cost, int sort) {
        PointsMallItem item = new PointsMallItem();
        item.setCode(code);
        item.setName(name);
        item.setIcon(icon);
        item.setCost(cost);
        item.setSortOrder(sort);
        item.setEnabled(1);
        itemRepository.save(item);
    }

    private List<Map<String, Object>> toItemMaps(List<PointsMallItem> items) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (PointsMallItem item : items) {
            list.add(toItemMap(item));
        }
        return list;
    }

    private Map<String, Object> toItemMap(PointsMallItem item) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", item.getId());
        m.put("code", item.getCode());
        m.put("name", item.getName());
        m.put("icon", item.getIcon());
        m.put("cost", item.getCost());
        return m;
    }

    private List<Map<String, Object>> toOrderMaps(List<PointsMallOrder> orders) {
        Map<Long, String> names = new HashMap<Long, String>();
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (PointsMallOrder order : orders) {
            String name = names.computeIfAbsent(order.getStudentId(), id ->
                    userRepository.findById(id).map(SysUser::getRealName).orElse("学生"));
            list.add(toOrderMap(order, name));
        }
        return list;
    }

    private Map<String, Object> toOrderMap(PointsMallOrder order, String studentName) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", order.getId());
        m.put("studentId", order.getStudentId());
        m.put("studentName", studentName);
        m.put("itemId", order.getItemId());
        m.put("itemName", order.getItemName());
        m.put("itemIcon", order.getItemIcon());
        m.put("points", order.getPoints());
        m.put("status", order.getStatus());
        m.put("createdAt", order.getCreatedAt());
        return m;
    }
}
