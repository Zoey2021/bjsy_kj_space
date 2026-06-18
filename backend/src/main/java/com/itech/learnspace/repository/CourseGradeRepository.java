package com.itech.learnspace.repository;

import com.itech.learnspace.entity.CourseGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseGradeRepository extends JpaRepository<CourseGrade, Long> {
    List<CourseGrade> findAllByOrderBySortOrderAsc();

    /** 按教材类型取册次列表（配套 / 校本） */
    List<CourseGrade> findByTextbookTypeOrderBySortOrderAsc(String textbookType);
}
