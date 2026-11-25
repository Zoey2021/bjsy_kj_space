package com.itech.learnspace.repository;

import com.itech.learnspace.entity.UnitQuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitQuizSubmissionRepository extends JpaRepository<UnitQuizSubmission, Long> {

    Optional<UnitQuizSubmission> findByLessonIdAndStudentId(Long lessonId, Long studentId);

    List<UnitQuizSubmission> findByLessonIdAndClassIdOrderBySubmittedAtDesc(Long lessonId, Long classId);

    List<UnitQuizSubmission> findByLessonIdOrderBySubmittedAtDesc(Long lessonId);
}
