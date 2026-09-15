package com.itech.learnspace.repository;

import com.itech.learnspace.entity.LessonScaffold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LessonScaffoldRepository extends JpaRepository<LessonScaffold, Long> {
    Optional<LessonScaffold> findByLessonId(Long lessonId);
}
