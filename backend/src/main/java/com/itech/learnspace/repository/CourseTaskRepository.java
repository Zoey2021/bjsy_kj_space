package com.itech.learnspace.repository;

import com.itech.learnspace.entity.CourseTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseTaskRepository extends JpaRepository<CourseTask, Long> {
    List<CourseTask> findByLessonIdOrderBySortOrderAsc(Long lessonId);
    List<CourseTask> findByLessonIdIn(List<Long> lessonIds);
}
