package com.eventitta.region.repository;

import com.eventitta.region.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, String> {
    List<Region> findByParentCodeIsNullOrderByNameAsc();

    List<Region> findByParentCodeOrderByNameAsc(String parentCode);

    // 리프 노드(최하위 지역)만 조회
    @Query("SELECT r FROM Region r WHERE r.code NOT IN (SELECT DISTINCT r2.parentCode FROM Region r2 WHERE r2.parentCode IS NOT NULL) ORDER BY r.code")
    List<Region> findLeafRegionsOrderByCode();

    // 특정 코드들의 상위 계층 조회
    @Query("SELECT r FROM Region r WHERE r.code IN :codes ORDER BY r.level, r.code")
    List<Region> findByCodeInOrderByLevelAndCode(List<String> codes);
}
