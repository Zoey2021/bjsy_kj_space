package com.itech.learnspace.repository;

import com.itech.learnspace.entity.SysTeacherClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface SysTeacherClassRepository extends JpaRepository<SysTeacherClass, Long> {

    List<SysTeacherClass> findByTeacherId(Long teacherId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByTeacherId(Long teacherId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByClassId(Long classId);
}
