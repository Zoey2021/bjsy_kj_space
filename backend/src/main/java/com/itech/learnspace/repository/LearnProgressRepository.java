package com.itech.learnspace.repository;

import com.itech.learnspace.entity.LearnProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearnProgressRepository extends JpaRepository<LearnProgress, Long> {
    Optional<LearnProgress> findByStudentIdAndLessonId(Long studentId, Long lessonId);
    List<LearnProgress> findByStudentId(Long studentId);
    List<LearnProgress> findByLessonId(Long lessonId);
}
