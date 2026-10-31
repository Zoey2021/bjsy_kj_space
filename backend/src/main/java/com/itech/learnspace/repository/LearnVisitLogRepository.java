package com.itech.learnspace.repository;

import com.itech.learnspace.entity.LearnVisitLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearnVisitLogRepository extends JpaRepository<LearnVisitLog, Long> {
    long deleteByLessonIdAndStudentIdIn(Long lessonId, List<Long> studentIds);
}
