package com.itech.learnspace.repository;

import com.itech.learnspace.entity.PointsMallItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PointsMallItemRepository extends JpaRepository<PointsMallItem, Long> {
    List<PointsMallItem> findByEnabledOrderBySortOrderAscIdAsc(Integer enabled);

    Optional<PointsMallItem> findByCode(String code);
}
