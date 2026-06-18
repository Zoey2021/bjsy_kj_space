package com.itech.learnspace.repository;

import com.itech.learnspace.entity.CourseLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseLessonRepository extends JpaRepository<CourseLesson, Long> {
    List<CourseLesson> findByUnitIdOrderBySortOrderAsc(Long unitId);
}
