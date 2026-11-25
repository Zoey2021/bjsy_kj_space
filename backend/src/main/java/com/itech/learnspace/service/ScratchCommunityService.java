package com.itech.learnspace.service;

import com.itech.learnspace.config.ActivityProperties;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.SysClassRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ScratchCommunityService {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_WITHDRAWN = "withdrawn";

    private final JdbcTemplate jdbcTemplate;
    private final SysClassRepository classRepository;
    private final ActivityProperties activityProperties;

    public ScratchCommunityService(JdbcTemplate jdbcTemplate,
                                   SysClassRepository classRepository,
                                   ActivityProperties activityProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.classRepository = classRepository;
        this.activityProperties = activityProperties;
    }

    @PostConstruct
    public void ensureSchema() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS scratch_project ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "title VARCHAR(120) NOT NULL,"
                + "author_id BIGINT NULL,"
                + "author_name VARCHAR(64) NOT NULL,"
                + "class_id BIGINT NULL,"
                + "class_name VARCHAR(64) NULL,"
                + "cover_url VARCHAR(500) NULL,"
                + "scratch_id VARCHAR(32) NULL,"
                + "sb3_path VARCHAR(500) NULL,"
                + "view_count INT NOT NULL DEFAULT 0,"
                + "like_count INT NOT NULL DEFAULT 0,"
                + "featured TINYINT NOT NULL DEFAULT 0,"
                + "published TINYINT NOT NULL DEFAULT 0,"
                + "status VARCHAR(20) NOT NULL DEFAULT 'pending',"
                + "reject_reason VARCHAR(200) NULL,"
                + "created_at DATETIME NOT NULL,"
                + "updated_at DATETIME NOT NULL"
                + ")");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS scratch_project_like ("
                + "project_id BIGINT NOT NULL,"
                + "student_id BIGINT NOT NULL,"
                + "created_at DATETIME NOT NULL,"
                + "PRIMARY KEY (project_id, student_id)"
                + ")");
        ensureColumn("scratch_project", "class_id", "BIGINT NULL");
        ensureColumn("scratch_project", "status", "VARCHAR(20) NOT NULL DEFAULT 'pending'");
        ensureColumn("scratch_project", "reject_reason", "VARCHAR(200) NULL");
        // 先补齐旧数据 status，再撤回所有没有 .sb3 的作品（含早期示范课）
        jdbcTemplate.update(
                "UPDATE scratch_project SET status = 'published' WHERE published = 1 AND (status IS NULL OR status = '')");
        jdbcTemplate.update(
                "UPDATE scratch_project SET status = 'pending' WHERE published = 0 AND (status IS NULL OR status = '')");
        jdbcTemplate.update(
                "UPDATE scratch_project SET status = ?, published = 0, updated_at = NOW() "
                        + "WHERE (sb3_path IS NULL OR TRIM(sb3_path) = '')",
                STATUS_WITHDRAWN);
        // 江辰兮内置样例：作品名「勇闯星际」、作者 2021级9班、封面图
        if (resolveExistingFile("/scratch-samples/jiangchenxi.sb3") != null) {
            final String sampleSb3 = "/scratch-samples/jiangchenxi.sb3";
            final String sampleCover = "/scratch-samples/jiangchenxi-cover.png";
            final String sampleTitle = "勇闯星际";
            final String sampleAuthor = "江辰兮";
            final String sampleClass = "2021级9班";
            int fixed = jdbcTemplate.update(
                    "UPDATE scratch_project SET title = ?, author_name = ?, class_name = ?, cover_url = ?, "
                            + "sb3_path = ?, status = ?, published = 1, featured = 1, "
                            + "reject_reason = NULL, updated_at = NOW() "
                            + "WHERE sb3_path = ? OR title LIKE '%江辰兮%' OR title = ?",
                    sampleTitle, sampleAuthor, sampleClass, sampleCover,
                    sampleSb3, STATUS_PUBLISHED, sampleSb3, sampleTitle);
            if (fixed == 0) {
                Integer n = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM scratch_project WHERE sb3_path = ?",
                        Integer.class, sampleSb3);
                if (n == null || n == 0) {
                    long id = insertProject(sampleTitle, null, sampleAuthor, null, sampleClass,
                            sampleSb3, 1, 1, STATUS_PUBLISHED);
                    jdbcTemplate.update(
                            "UPDATE scratch_project SET cover_url = ?, updated_at = NOW() WHERE id = ?",
                            sampleCover, id);
                }
            }
        }
    }

    private void ensureColumn(String table, String column, String ddl) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (n == null || n == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl);
        }
    }

    /** 社区广场：仅已发布（老师推荐优先） */
    public List<Map<String, Object>> list(String sort, SysUser viewer) {
        String order = "hot".equalsIgnoreCase(sort)
                ? "featured DESC, like_count DESC, view_count DESC, id DESC"
                : "featured DESC, id DESC";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM scratch_project WHERE status = ? AND published = 1 "
                        + "AND sb3_path IS NOT NULL AND TRIM(sb3_path) <> '' ORDER BY " + order,
                STATUS_PUBLISHED);
        Long viewerId = studentId(viewer);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            out.add(card(row, viewerId, false, null));
        }
        return out;
    }

    /** 学生自己的投稿 */
    public List<Map<String, Object>> mine(SysUser student) {
        if (student == null || !"STUDENT".equals(student.getRole())) {
            throw new BusinessException(403, "请用学生账号查看");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM scratch_project WHERE author_id = ? ORDER BY id DESC", student.getId());
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            out.add(card(row, student.getId(), false, null));
        }
        return out;
    }

    /** 教师审核列表 */
    public List<Map<String, Object>> teacherList(String status) {
        String st = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> rows;
        if ("pending".equals(st) || "published".equals(st) || "rejected".equals(st) || "withdrawn".equals(st)) {
            rows = jdbcTemplate.queryForList(
                    "SELECT * FROM scratch_project WHERE status = ? ORDER BY id DESC", st);
        } else {
            rows = jdbcTemplate.queryForList(
                    "SELECT * FROM scratch_project ORDER BY "
                            + "FIELD(status,'pending','published','rejected','withdrawn'), id DESC");
        }
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            out.add(card(row, null, false, null));
        }
        return out;
    }

    public Map<String, Object> teacherCounts() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("pending", countStatus(STATUS_PENDING));
        m.put("published", countStatus(STATUS_PUBLISHED));
        m.put("rejected", countStatus(STATUS_REJECTED));
        m.put("withdrawn", countStatus(STATUS_WITHDRAWN));
        return m;
    }

    private int countStatus(String status) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM scratch_project WHERE status = ?", Integer.class, status);
        return n == null ? 0 : n;
    }

    @Transactional
    public Map<String, Object> detail(long id, SysUser viewer, String publicOrigin) {
        Map<String, Object> row = oneAny(id);
        String status = str(row.get("status"));
        boolean published = intVal(row.get("published")) == 1 && STATUS_PUBLISHED.equals(status);
        boolean owner = viewer != null && "STUDENT".equals(viewer.getRole())
                && row.get("author_id") != null
                && viewer.getId().equals(((Number) row.get("author_id")).longValue());
        boolean teacher = viewer != null
                && ("TEACHER".equals(viewer.getRole()) || "ADMIN".equals(viewer.getRole()));
        if (!published && !owner && !teacher) {
            throw new BusinessException(404, "作品不存在或尚未发布");
        }
        if (published) {
            jdbcTemplate.update("UPDATE scratch_project SET view_count = view_count + 1 WHERE id = ?", id);
            row.put("view_count", intVal(row.get("view_count")) + 1);
        }
        return card(row, studentId(viewer), true, publicOrigin);
    }

    @Transactional
    public Map<String, Object> toggleLike(long id, SysUser student) {
        if (student == null || !"STUDENT".equals(student.getRole())) {
            throw new BusinessException(403, "请用学生账号点赞");
        }
        Map<String, Object> row = onePublished(id);
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM scratch_project_like WHERE project_id = ? AND student_id = ?",
                Integer.class, id, student.getId());
        boolean liked;
        if (exists != null && exists > 0) {
            jdbcTemplate.update("DELETE FROM scratch_project_like WHERE project_id = ? AND student_id = ?",
                    id, student.getId());
            jdbcTemplate.update("UPDATE scratch_project SET like_count = GREATEST(like_count - 1, 0) WHERE id = ?", id);
            liked = false;
        } else {
            jdbcTemplate.update(
                    "INSERT INTO scratch_project_like (project_id, student_id, created_at) VALUES (?,?,?)",
                    id, student.getId(), Timestamp.valueOf(LocalDateTime.now()));
            jdbcTemplate.update("UPDATE scratch_project SET like_count = like_count + 1 WHERE id = ?", id);
            liked = true;
        }
        Map<String, Object> card = card(oneAny(id), student.getId(), true, null);
        card.put("liked", liked);
        card.put("message", liked ? "已点赞" : "已取消点赞");
        return card;
    }

    @Transactional
    public Map<String, Object> submit(SysUser student, String title, MultipartFile file) {
        if (student == null || !"STUDENT".equals(student.getRole())) {
            throw new BusinessException(403, "请用学生账号投稿");
        }
        String t = normalizeTitle(title);
        String publicPath = saveSb3(file);

        Long classId = student.getClassId();
        String className = null;
        if (classId != null) {
            className = classRepository.findById(classId).map(c -> c.getName()).orElse(null);
        }

        long id = insertProject(
                t,
                student.getId(),
                student.getRealName() != null ? student.getRealName() : student.getUsername(),
                classId,
                className,
                publicPath,
                0,
                0,
                STATUS_PENDING);
        Map<String, Object> card = card(oneAny(id), student.getId(), false, null);
        card.put("message", "已提交，等待老师审核");
        return card;
    }

    /**
     * 教师直接上传并发布到编程作品社区，学生可见、可玩、可点赞。
     */
    @Transactional
    public Map<String, Object> teacherPublish(SysUser teacher, String title, MultipartFile file) {
        if (teacher == null || !("TEACHER".equals(teacher.getRole()) || "ADMIN".equals(teacher.getRole()))) {
            throw new BusinessException(403, "仅教师可上传发布");
        }
        String t = normalizeTitle(title);
        String publicPath = saveSb3(file);
        String authorName = teacher.getRealName() != null && !teacher.getRealName().trim().isEmpty()
                ? teacher.getRealName().trim() + "（老师）"
                : "老师推荐";
        long id = insertProject(
                t,
                teacher.getId(),
                authorName,
                null,
                "老师推荐",
                publicPath,
                1,
                1,
                STATUS_PUBLISHED);
        Map<String, Object> card = card(oneAny(id), null, true, null);
        card.put("message", "已发布到编程作品社区");
        return card;
    }

    private String normalizeTitle(String title) {
        String t = title == null ? "" : title.trim();
        if (t.isEmpty() || t.length() > 40) {
            throw new BusinessException("请为作品取名（1～40 个字）");
        }
        return t;
    }

    private String saveSb3(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 Scratch 作品文件（.sb3）");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!original.endsWith(".sb3")) {
            throw new BusinessException("请上传 .sb3 文件（在 Scratch 中：文件 → 保存到电脑）");
        }
        if (file.getSize() > 40L * 1024 * 1024) {
            throw new BusinessException("文件太大，请控制在 40MB 以内");
        }
        String fileName = "p" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".sb3";
        Path dir = Paths.get(activityProperties.getHtmlOutputDir(), "scratch");
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName).toFile());
        } catch (IOException e) {
            throw new BusinessException("上传失败，请稍后再试");
        }
        return "/lessons/generated/scratch/" + fileName;
    }

    private long insertProject(String title, Long authorId, String authorName, Long classId, String className,
                               String sb3Path, int featured, int published, String status) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keys = new GeneratedKeyHolder();
        final String finalClassName = className;
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO scratch_project (title, author_id, author_name, class_id, class_name, cover_url, scratch_id,"
                            + " sb3_path, view_count, like_count, featured, published, status, reject_reason, created_at, updated_at)"
                            + " VALUES (?,?,?,?,?,?,NULL,?,0,0,?,?,?,NULL,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, title);
            if (authorId == null) {
                ps.setObject(2, null);
            } else {
                ps.setLong(2, authorId);
            }
            ps.setString(3, authorName);
            if (classId == null) {
                ps.setObject(4, null);
            } else {
                ps.setLong(4, classId);
            }
            ps.setString(5, finalClassName);
            ps.setString(6, "");
            ps.setString(7, sb3Path);
            ps.setInt(8, featured);
            ps.setInt(9, published);
            ps.setString(10, status);
            ps.setTimestamp(11, Timestamp.valueOf(now));
            ps.setTimestamp(12, Timestamp.valueOf(now));
            return ps;
        }, keys);
        Number key = keys.getKey();
        if (key == null) {
            throw new BusinessException("提交失败，请稍后再试");
        }
        return key.longValue();
    }

    @Transactional
    public Map<String, Object> review(long id, SysUser teacher, String action, String reason) {
        if (teacher == null || !("TEACHER".equals(teacher.getRole()) || "ADMIN".equals(teacher.getRole()))) {
            throw new BusinessException(403, "仅教师可审核");
        }
        Map<String, Object> row = oneAny(id);
        String act = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        if ("approve".equals(act) || "publish".equals(act)) {
            jdbcTemplate.update(
                    "UPDATE scratch_project SET status = ?, published = 1, reject_reason = NULL, updated_at = ? WHERE id = ?",
                    STATUS_PUBLISHED, Timestamp.valueOf(now), id);
        } else if ("reject".equals(act)) {
            String r = reason == null ? "" : reason.trim();
            if (r.length() > 100) {
                r = r.substring(0, 100);
            }
            jdbcTemplate.update(
                    "UPDATE scratch_project SET status = ?, published = 0, reject_reason = ?, updated_at = ? WHERE id = ?",
                    STATUS_REJECTED, r.isEmpty() ? "请修改后重新上传" : r, Timestamp.valueOf(now), id);
        } else if ("withdraw".equals(act)) {
            jdbcTemplate.update(
                    "UPDATE scratch_project SET status = ?, published = 0, updated_at = ? WHERE id = ?",
                    STATUS_WITHDRAWN, Timestamp.valueOf(now), id);
        } else {
            throw new BusinessException("未知操作");
        }
        Map<String, Object> card = card(oneAny(id), null, false, null);
        if ("approve".equals(act) || "publish".equals(act)) {
            card.put("message", "已发布到学生社区");
        } else if ("reject".equals(act)) {
            card.put("message", "已退回");
        } else {
            card.put("message", "已撤回");
        }
        return card;
    }

    private Map<String, Object> onePublished(long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM scratch_project WHERE id = ? AND status = ? AND published = 1",
                id, STATUS_PUBLISHED);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "作品不存在或尚未发布");
        }
        return rows.get(0);
    }

    private Map<String, Object> oneAny(long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM scratch_project WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "作品不存在");
        }
        return rows.get(0);
    }

    private Map<String, Object> card(Map<String, Object> row, Long viewerId, boolean withPlay, String publicOrigin) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        long id = ((Number) row.get("id")).longValue();
        String sb3 = row.get("sb3_path") == null ? "" : String.valueOf(row.get("sb3_path"));
        String status = str(row.get("status"));
        m.put("id", id);
        m.put("title", row.get("title"));
        m.put("authorName", row.get("author_name"));
        m.put("className", row.get("class_name"));
        m.put("coverUrl", row.get("cover_url"));
        m.put("sb3Path", sb3);
        m.put("viewCount", intVal(row.get("view_count")));
        m.put("likeCount", intVal(row.get("like_count")));
        m.put("featured", intVal(row.get("featured")) == 1);
        m.put("status", status);
        m.put("statusLabel", statusLabel(status));
        m.put("rejectReason", row.get("reject_reason"));
        m.put("published", intVal(row.get("published")) == 1);
        m.put("createdAt", row.get("created_at"));
        m.put("updatedAt", row.get("updated_at"));
        if (viewerId != null) {
            Integer liked = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM scratch_project_like WHERE project_id = ? AND student_id = ?",
                    Integer.class, id, viewerId);
            m.put("liked", liked != null && liked > 0);
        } else {
            m.put("liked", false);
        }
        if (withPlay && sb3 != null && !sb3.isEmpty()) {
            // 一律走后端 API：iframe 内可带登录头；静态 /scratch-samples 易因文件权限被 nginx 403
            boolean published = STATUS_PUBLISHED.equals(status) && intVal(row.get("published")) == 1;
            String fetchSb3 = published
                    ? "/api/scratch/projects/" + id + "/sb3"
                    : "/api/scratch/teacher/projects/" + id + "/sb3";
            m.put("playUrl", "/scratch-player/index.html?sb3=" + encode(fetchSb3) + "&fit=1");
            m.put("sb3Url", fetchSb3);
            // 学生端不提供下载；教师审核页自行拼 teacher sb3 地址
            m.put("sb3StaticPath", sb3);
        }
        return m;
    }

    /** 已发布作品的磁盘路径；未发布或不存在则抛错 */
    public Path resolvePublishedSb3File(long id) {
        Map<String, Object> row = onePublished(id);
        return requireSb3File(row, "服务器上找不到该作品文件，请老师重新上传");
    }

    /** 教师查看任意状态作品文件（审核试玩） */
    public Path resolveAnySb3File(long id) {
        Map<String, Object> row = oneAny(id);
        return requireSb3File(row, "服务器上找不到该作品文件");
    }

    private Path requireSb3File(Map<String, Object> row, String missingMsg) {
        String sb3 = row.get("sb3_path") == null ? "" : String.valueOf(row.get("sb3_path")).trim();
        if (sb3.isEmpty()) {
            throw new BusinessException(404, "作品文件不存在");
        }
        Path file = resolveExistingFile(sb3);
        if (file == null) {
            throw new BusinessException(404, missingMsg);
        }
        return file;
    }

    /**
     * 解析作品磁盘路径：
     * - /lessons/generated/... → lesson-generated 卷
     * - /scratch-samples/... → 后端挂载的样例目录（或常见候选路径）
     */
    Path resolveExistingFile(String sb3Path) {
        if (sb3Path == null) {
            return null;
        }
        String sb3 = sb3Path.trim();
        if (sb3.isEmpty()) {
            return null;
        }
        if (sb3.startsWith("/lessons/generated/")) {
            Path file = Paths.get(activityProperties.getHtmlOutputDir(), sb3.substring("/lessons/generated/".length()));
            return Files.isRegularFile(file) ? file : null;
        }
        if (sb3.startsWith("/scratch-samples/")) {
            String relative = sb3.substring("/scratch-samples/".length());
            Path[] candidates = new Path[]{
                    Paths.get("/app/scratch-samples", relative),
                    Paths.get(activityProperties.getHtmlOutputDir(), "scratch-samples", relative),
                    Paths.get("frontend/public/scratch-samples", relative),
                    Paths.get("scratch-samples", relative)
            };
            for (Path file : candidates) {
                if (Files.isRegularFile(file)) {
                    return file;
                }
            }
            return null;
        }
        return null;
    }

    private String statusLabel(String status) {
        if (STATUS_PENDING.equals(status)) return "待审核";
        if (STATUS_PUBLISHED.equals(status)) return "已发布";
        if (STATUS_REJECTED.equals(status)) return "已退回";
        if (STATUS_WITHDRAWN.equals(status)) return "已撤回";
        return status == null ? "" : status;
    }

    private String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private Long studentId(SysUser viewer) {
        return viewer != null && "STUDENT".equals(viewer.getRole()) ? viewer.getId() : null;
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private int intVal(Object v) {
        if (v == null) {
            return 0;
        }
        return ((Number) v).intValue();
    }
}
