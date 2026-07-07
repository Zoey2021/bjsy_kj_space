package com.itech.learnspace.repository;

import com.itech.learnspace.entity.CourseActivityPublish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseActivityPublishRepository extends JpaRepository<CourseActivityPublish, Long> {
    Optional<CourseActivityPublish> findByLessonIdAndClassId(Long lessonId, Long classId);
}
