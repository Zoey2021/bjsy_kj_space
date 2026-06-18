package com.itech.learnspace.repository;

import com.itech.learnspace.entity.LearnPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LearnPointsRepository extends JpaRepository<LearnPoints, Long> {
    List<LearnPoints> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    boolean existsByStudentIdAndSourceTypeAndSourceId(Long studentId, String sourceType, Long sourceId);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM LearnPoints p WHERE p.studentId = ?1")
    Integer sumPointsByStudentId(Long studentId);

    @Query("SELECT p.studentId, SUM(p.points) FROM LearnPoints p WHERE p.studentId IN ?1 GROUP BY p.studentId ORDER BY SUM(p.points) DESC")
    List<Object[]> rankByClassStudents(List<Long> studentIds);
}
