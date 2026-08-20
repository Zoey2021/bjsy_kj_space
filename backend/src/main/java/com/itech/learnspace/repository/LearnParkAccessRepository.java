package com.itech.learnspace.repository;

import com.itech.learnspace.entity.LearnParkAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearnParkAccessRepository extends JpaRepository<LearnParkAccess, Long> {
    Optional<LearnParkAccess> findByStudentId(Long studentId);

    List<LearnParkAccess> findByClassIdOrderByAppliedAtDesc(Long classId);

    List<LearnParkAccess> findByClassIdAndStatusOrderByAppliedAtAsc(Long classId, String status);

    long countByClassIdAndStatus(Long classId, String status);
}
