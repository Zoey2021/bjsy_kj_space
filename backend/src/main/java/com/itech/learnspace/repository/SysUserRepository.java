package com.itech.learnspace.repository;

import com.itech.learnspace.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);
    List<SysUser> findByRole(String role);
    List<SysUser> findByClassId(Long classId);
    List<SysUser> findByRoleAndStatus(String role, Integer status);
    List<SysUser> findByClassIdAndRoleAndStatus(Long classId, String role, Integer status);

    @Query(value = "SELECT DISTINCT u.id FROM sys_user u "
            + "LEFT JOIN sys_class_student cs ON cs.student_id = u.id "
            + "WHERE u.role = 'STUDENT' AND (u.class_id = ?1 OR cs.class_id = ?1)",
            nativeQuery = true)
    List<Number> findStudentIdsInClass(Long classId);

    List<SysUser> findByRoleAndRealNameContaining(String role, String realName);
}
