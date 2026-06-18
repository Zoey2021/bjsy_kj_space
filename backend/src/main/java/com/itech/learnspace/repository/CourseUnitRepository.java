package com.itech.learnspace.repository;

import com.itech.learnspace.entity.CourseUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseUnitRepository extends JpaRepository<CourseUnit, Long> {
    List<CourseUnit> findByGradeIdOrderBySortOrderAsc(Long gradeId);
}
