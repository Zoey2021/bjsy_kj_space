package com.itech.learnspace.repository;

import com.itech.learnspace.entity.PointsMallOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointsMallOrderRepository extends JpaRepository<PointsMallOrder, Long> {
    List<PointsMallOrder> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<PointsMallOrder> findByClassIdOrderByCreatedAtDesc(Long classId);
}
