package com.itech.learnspace.repository;

import com.itech.learnspace.entity.LearnNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LearnNotificationRepository extends JpaRepository<LearnNotification, Long> {

    List<LearnNotification> findByTargetStudentIdAndCreatedAtAfterOrderByCreatedAtAsc(
            Long targetStudentId, LocalDateTime since);

    List<LearnNotification> findByTargetStudentIdIsNullAndClassIdAndCreatedAtAfterOrderByCreatedAtAsc(
            Long classId, LocalDateTime since);

    List<LearnNotification> findByTargetStudentIdOrderByCreatedAtDesc(Long targetStudentId);
}
