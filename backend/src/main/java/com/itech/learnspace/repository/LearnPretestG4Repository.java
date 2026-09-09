package com.itech.learnspace.repository;

import com.itech.learnspace.entity.LearnPretestG4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearnPretestG4Repository extends JpaRepository<LearnPretestG4, Long> {
    Optional<LearnPretestG4> findByStudentId(Long studentId);

    List<LearnPretestG4> findByClassIdOrderBySubmittedAtDesc(Long classId);
}
