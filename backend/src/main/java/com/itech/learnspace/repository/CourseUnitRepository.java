package com.itech.learnspace.repository;

import com.itech.learnspace.entity.CourseUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseUnitRepository extends JpaRepository<CourseUnit, Long> {
    List<CourseUnit> findByGradeIdOrderBySortOrderAsc(Long gradeId);

    /** 仅判断是否有单元，避免 listTextbooks 为 hasResource 拉全量单元造成 N+1 卡顿 */
    boolean existsByGradeId(Long gradeId);
}
