package com.itech.learnspace.repository;

import com.itech.learnspace.entity.CourseResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseResourceRepository extends JpaRepository<CourseResource, Long> {
    List<CourseResource> findByLessonIdOrderBySortOrderAsc(Long lessonId);
}
