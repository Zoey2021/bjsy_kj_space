package com.itech.learnspace.repository;

import com.itech.learnspace.entity.SysClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysClassRepository extends JpaRepository<SysClass, Long> {
    List<SysClass> findByTeacherId(Long teacherId);

    java.util.Optional<SysClass> findByLoginCode(String loginCode);
}
