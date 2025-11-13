package com.eventitta.region.service;

import com.eventitta.region.domain.Region;
import com.eventitta.region.dto.response.RegionOptionResponse;
import com.eventitta.region.dto.response.RegionResponse;
import com.eventitta.region.exception.RegionErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.eventitta.common.config.cache.CacheConstants.REGION_OPTIONS;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {
    private final RegionCacheService cacheService;

    /**
     * 최상위 지역(시/도) 목록 조회
     *
     * <p>캐싱된 Map에서 필터링 </p>
     *
     * @return 시/도 목록 (이름 기준 오름차순)
     */
    public List<RegionResponse> getTopLevelRegions() {
        Map<String, Region> regionMap = cacheService.getAllRegionsAsMap();

        return regionMap.values().stream()
            .filter(region -> region.getParentCode() == null)
            .sorted(Comparator.comparing(Region::getName))
            .map(RegionResponse::from)
            .toList();
    }

    /**
     * 특정 지역의 하위 지역 목록 조회
     *
     * <p>캐싱된 Map에서 필터링 </p>
     *
     * @param parentCode 상위 지역 코드
     * @return 하위 지역 목록 (이름 기준 오름차순)
     */
    public List<RegionResponse> getChildRegions(String parentCode) {
        Map<String, Region> regionMap = cacheService.getAllRegionsAsMap();

        return regionMap.values().stream()
            .filter(region -> parentCode.equals(region.getParentCode()))
            .sorted(Comparator.comparing(Region::getName))
            .map(RegionResponse::from)
            .toList();
    }

    /**
     * 특정 지역의 전체 계층 구조 조회
     *
     * <p>예: 청운효자동 → [서울특별시, 종로구, 청운효자동]</p>
     * <p>캐싱된 Map에서 탐색 </p>
     *
     * @param code 조회할 지역 코드
     * @return 최상위부터 해당 지역까지의 계층 리스트
     * @throws com.eventitta.region.exception.RegionException 존재하지 않는 지역 코드인 경우
     */
    public List<RegionResponse> getRegionHierarchy(String code) {
        Map<String, Region> regionMap = cacheService.getAllRegionsAsMap();
        Region region = findRegionInMap(code, regionMap);
        return buildHierarchyFromMap(region, regionMap);
    }

    /**
     * 셀렉트박스용 전체 지역 옵션 조회
     *
     * <p>최하위 지역(리프 노드)만 반환하며, 각 지역의 전체 경로 정보를 포함합니다.</p>
     * <p>자주 호출되고 계산 비용이 높아 별도 캐싱 </p>
     *
     * @return 리프 노드 지역 옵션 목록 (코드 기준 오름차순)
     */
    @Cacheable(value = REGION_OPTIONS, key = "'leafRegions'")
    public List<RegionOptionResponse> getRegionOptions() {
        Map<String, Region> regionMap = cacheService.getAllRegionsAsMap();
        List<Region> leafRegions = getLeafRegions(regionMap);

        return leafRegions.stream()
            .map(leaf -> createRegionOption(leaf, regionMap))
            .toList();
    }

    /**
     * 리프 노드(최하위 지역)만 필터링
     *
     * <p>DB 쿼리 대신 메모리에서 처리</p>
     *
     * @param allRegions 전체 지역 Map (캐시에서 조회)
     * @return 리프 노드 목록 (코드 기준 오름차순)
     */
    private List<Region> getLeafRegions(Map<String, Region> allRegions) {
        // 1. 모든 parentCode 수집
        Set<String> parentCodes = allRegions.values().stream()
            .map(Region::getParentCode)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // 2. parentCode에 없는 지역 = 리프 노드
        return allRegions.values().stream()
            .filter(region -> !parentCodes.contains(region.getCode()))
            .sorted(Comparator.comparing(Region::getCode))
            .toList();
    }

    private Region findRegionInMap(String code, Map<String, Region> regionMap) {
        return Optional.ofNullable(regionMap.get(code))
            .orElseThrow(RegionErrorCode.NOT_FOUND_REGION_CODE::defaultException);
    }

    private List<RegionResponse> buildHierarchyFromMap(Region region, Map<String, Region> regionMap) {
        List<RegionResponse> result = new ArrayList<>();
        collectHierarchyFromMap(region, regionMap, result);
        Collections.reverse(result);
        return result;
    }

    private void collectHierarchyFromMap(Region region, Map<String, Region> regionMap, List<RegionResponse> result) {
        Region current = region;
        while (current != null) {
            result.add(RegionResponse.from(current));
            current = getParentFromMap(current, regionMap);
        }
    }

    private Region getParentFromMap(Region region, Map<String, Region> regionMap) {
        String parentCode = region.getParentCode();
        return parentCode != null ? regionMap.get(parentCode) : null;
    }

    private RegionOptionResponse createRegionOption(Region leaf, Map<String, Region> regionMap) {
        List<String> hierarchyCodes = buildHierarchyPath(leaf, regionMap);
        List<String> hierarchyNames = hierarchyCodes.stream()
            .map(code -> regionMap.get(code).getName())
            .toList();

        String displayName = String.join(" > ", hierarchyNames);

        return new RegionOptionResponse(
            leaf.getCode(),
            displayName,
            hierarchyCodes,
            hierarchyNames
        );
    }

    private List<String> buildHierarchyPath(Region leaf, Map<String, Region> regionMap) {
        List<String> hierarchyCodes = new ArrayList<>();
        collectHierarchyCodes(leaf.getCode(), regionMap, hierarchyCodes);
        Collections.reverse(hierarchyCodes);
        return hierarchyCodes;
    }

    private void collectHierarchyCodes(String currentCode, Map<String, Region> regionMap, List<String> hierarchyCodes) {
        while (currentCode != null) {
            Region currentRegion = regionMap.get(currentCode);
            if (currentRegion == null) break;

            hierarchyCodes.add(currentCode);
            currentCode = currentRegion.getParentCode();
        }
    }
}
