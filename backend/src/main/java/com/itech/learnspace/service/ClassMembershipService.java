package com.itech.learnspace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service
public class ClassMembershipService {

    private static final Logger log = LoggerFactory.getLogger(ClassMembershipService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void replace(Long studentId, Long classId) {
        if (studentId == null) {
            return;
        }
        try {
            if (!tableExists("sys_class_student")) {
                return;
            }
            entityManager.createNativeQuery("DELETE FROM sys_class_student WHERE student_id = ?1")
                    .setParameter(1, studentId)
                    .executeUpdate();
            if (classId == null) {
                return;
            }
            entityManager.createNativeQuery(
                    "INSERT INTO sys_class_student (class_id, student_id, joined_at) VALUES (?1, ?2, NOW())")
                    .setParameter(1, classId)
                    .setParameter(2, studentId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("sync class membership failed, studentId={}, classId={}: {}", studentId, classId, e.getMessage());
        }
    }

    private boolean tableExists(String table) {
        Number count = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?1")
                .setParameter(1, table)
                .getSingleResult();
        return count != null && count.intValue() > 0;
    }
}
