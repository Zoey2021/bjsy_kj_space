package com.itech.learnspace.repository;

import com.itech.learnspace.entity.LearnParkPlay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LearnParkPlayRepository extends JpaRepository<LearnParkPlay, Long> {

    @Query("SELECT p.gameId, p.gameTitle, COUNT(DISTINCT p.studentId), COUNT(p.id), COALESCE(SUM(p.durationSeconds), 0) "
            + "FROM LearnParkPlay p WHERE p.classId = :classId AND p.lessonId = :lessonId "
            + "GROUP BY p.gameId, p.gameTitle ORDER BY SUM(p.durationSeconds) DESC")
    List<Object[]> summarizeByClassAndLesson(@Param("classId") Long classId, @Param("lessonId") Long lessonId);

    List<LearnParkPlay> findTop40ByClassIdAndLessonIdOrderByPlayedAtDesc(Long classId, Long lessonId);
}
