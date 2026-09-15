package com.itech.learnspace.repository;

import com.itech.learnspace.entity.LearnHintLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LearnHintLogRepository extends JpaRepository<LearnHintLog, Long> {
    boolean existsByStudentIdAndLessonIdAndActivityIndexAndHintIndex(
            Long studentId, Long lessonId, Integer activityIndex, Integer hintIndex);

    List<LearnHintLog> findByLessonId(Long lessonId);

    @Query("SELECT COUNT(h) FROM LearnHintLog h WHERE h.studentId = ?1 AND h.lessonId = ?2 AND h.activityIndex = ?3")
    long countByStudentAndActivity(Long studentId, Long lessonId, Integer activityIndex);
}
