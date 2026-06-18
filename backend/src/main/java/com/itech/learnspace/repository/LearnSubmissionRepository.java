package com.itech.learnspace.repository;

import com.itech.learnspace.entity.LearnSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearnSubmissionRepository extends JpaRepository<LearnSubmission, Long> {
    Optional<LearnSubmission> findByTaskIdAndStudentId(Long taskId, Long studentId);
    List<LearnSubmission> findByStudentId(Long studentId);
    List<LearnSubmission> findByTaskIdIn(List<Long> taskIds);
    List<LearnSubmission> findByStudentIdIn(List<Long> studentIds);
    long countByTaskIdInAndStudentIdIn(List<Long> taskIds, List<Long> studentIds);
}
