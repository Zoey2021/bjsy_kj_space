package com.itech.learnspace.repository;

import com.itech.learnspace.entity.LearnSubmissionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearnSubmissionLogRepository extends JpaRepository<LearnSubmissionLog, Long> {
    List<LearnSubmissionLog> findByStudentIdAndLessonIdOrderByCreatedAtDesc(Long studentId, Long lessonId);

    List<LearnSubmissionLog> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
