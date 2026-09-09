package com.itech.learnspace.repository;

import com.itech.learnspace.entity.LearnPretestG6;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearnPretestG6Repository extends JpaRepository<LearnPretestG6, Long> {
    Optional<LearnPretestG6> findByStudentId(Long studentId);

    List<LearnPretestG6> findByClassIdOrderBySubmittedAtDesc(Long classId);

    long deleteByClassId(Long classId);
}
