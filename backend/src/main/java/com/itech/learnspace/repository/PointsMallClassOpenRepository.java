package com.itech.learnspace.repository;

import com.itech.learnspace.entity.PointsMallClassOpen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointsMallClassOpenRepository extends JpaRepository<PointsMallClassOpen, Long> {
    Optional<PointsMallClassOpen> findByClassId(Long classId);
}
