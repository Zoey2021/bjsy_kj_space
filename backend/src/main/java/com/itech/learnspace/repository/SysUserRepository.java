package com.itech.learnspace.repository;

import com.itech.learnspace.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);
    List<SysUser> findByRole(String role);
    List<SysUser> findByClassId(Long classId);
    List<SysUser> findByRoleAndStatus(String role, Integer status);
    List<SysUser> findByClassIdAndRoleAndStatus(Long classId, String role, Integer status);
}
