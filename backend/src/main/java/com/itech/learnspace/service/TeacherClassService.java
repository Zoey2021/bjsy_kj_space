package com.itech.learnspace.service;

import com.itech.learnspace.entity.SysClass;
import com.itech.learnspace.entity.SysTeacherClass;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.SysClassRepository;
import com.itech.learnspace.repository.SysTeacherClassRepository;
import com.itech.learnspace.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TeacherClassService {

    private final SysUserRepository userRepository;
    private final SysClassRepository classRepository;
    private final SysTeacherClassRepository matchRepository;
    private final AccountAccessService accountAccessService;

    public TeacherClassService(SysUserRepository userRepository,
                               SysClassRepository classRepository,
                               SysTeacherClassRepository matchRepository,
                               AccountAccessService accountAccessService) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.matchRepository = matchRepository;
        this.accountAccessService = accountAccessService;
    }

    /** 没有匹配记录时返回 null，表示仍可查看全部班级。已保存匹配的账号只看这些班。 */
    public Set<Long> allowedClassIds(Long userId) {
        requireUser(userId);
        List<Long> ids = matchIds(userId);
        if (ids.isEmpty()) {
            return null;
        }
        return new HashSet<Long>(ids);
    }

    public List<SysClass> visibleClasses(Long userId) {
        List<SysClass> all = new ArrayList<SysClass>(classRepository.findAll());
        all.sort(classOrder());
        Set<Long> allowed = allowedClassIds(userId);
        if (allowed == null) {
            return all;
        }
        List<SysClass> kept = new ArrayList<SysClass>();
        for (SysClass clazz : all) {
            if (allowed.contains(clazz.getId())) {
                kept.add(clazz);
            }
        }
        return kept;
    }

    public void assertCanView(Long userId, Long classId) {
        if (classId == null || !classRepository.findById(classId).isPresent()) {
            throw new BusinessException("班级不存在");
        }
        Set<Long> allowed = allowedClassIds(userId);
        if (allowed != null && !allowed.contains(classId)) {
            throw new BusinessException(403, "只能查看已匹配的班级");
        }
    }

    public void assertCanAssign(SysUser operator) {
        if (operator != null && ("ADMIN".equals(operator.getRole())
                || AccountAccessService.ALL.equals(accountAccessService.scope(operator)))) {
            return;
        }
        throw new BusinessException(403, "只有学校管理人员可以匹配班级");
    }

    public boolean canAssign(SysUser operator) {
        return operator != null && ("ADMIN".equals(operator.getRole())
                || AccountAccessService.ALL.equals(accountAccessService.scope(operator)));
    }

    public List<Long> matchIds(Long teacherId) {
        List<Long> ids = new ArrayList<Long>();
        for (SysTeacherClass row : matchRepository.findByTeacherId(teacherId)) {
            if (row.getClassId() != null) {
                ids.add(row.getClassId());
            }
        }
        return ids;
    }

    @Transactional
    public void replaceMatches(Long teacherId, List<Long> classIds) {
        requireTeacher(teacherId);
        LinkedHashSet<Long> unique = new LinkedHashSet<Long>();
        if (classIds != null) {
            for (Long classId : classIds) {
                if (classId != null) {
                    unique.add(classId);
                }
            }
        }
        for (Long classId : unique) {
            if (!classRepository.findById(classId).isPresent()) {
                throw new BusinessException("班级不存在");
            }
        }
        matchRepository.deleteByTeacherId(teacherId);
        matchRepository.flush();
        for (Long classId : unique) {
            SysTeacherClass row = new SysTeacherClass();
            row.setTeacherId(teacherId);
            row.setClassId(classId);
            matchRepository.save(row);
        }
    }

    @Transactional
    public void deleteByTeacher(Long teacherId) {
        matchRepository.deleteByTeacherId(teacherId);
    }

    @Transactional
    public void deleteByClass(Long classId) {
        matchRepository.deleteByClassId(classId);
    }

    public String classLabel(SysClass clazz) {
        if (clazz == null) {
            return "";
        }
        String grade = clazz.getGradeName() == null ? "" : clazz.getGradeName().trim();
        String name = clazz.getName() == null ? "" : clazz.getName().trim();
        if (grade.isEmpty() || name.contains(grade)) {
            return name;
        }
        return grade + " " + name;
    }

    private SysUser requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    private void requireTeacher(Long teacherId) {
        SysUser user = requireUser(teacherId);
        if (!"TEACHER".equals(user.getRole())) {
            throw new BusinessException("只能为教师匹配班级");
        }
    }

    private Comparator<SysClass> classOrder() {
        return Comparator
                .comparing((SysClass c) -> c.getGradeName() == null ? "" : c.getGradeName())
                .thenComparing(c -> c.getName() == null ? "" : c.getName());
    }
}
