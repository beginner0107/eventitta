package com.eventitta.region.service;

import com.eventitta.region.domain.Region;
import com.eventitta.region.dto.RegionDto;
import com.eventitta.region.dto.RegionOptionDto;
import com.eventitta.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {
    private final RegionRepository regionRepository;

    public List<RegionDto> getTopLevelRegions() {
        return regionRepository.findByParentCodeIsNullOrderByNameAsc()
            .stream()
            .map(RegionDto::from)
            .collect(Collectors.toList());
    }

    public List<RegionDto> getChildRegions(String parentCode) {
        return regionRepository.findByParentCodeOrderByNameAsc(parentCode)
            .stream()
            .map(RegionDto::from)
            .collect(Collectors.toList());
    }

    public List<RegionDto> getRegionHierarchy(String code) {
        Map<String, Region> regionMap = createRegionMap();
        Region region = findRegionInMap(code, regionMap);
        return buildHierarchyFromMap(region, regionMap);
    }

    public List<RegionOptionDto> getRegionOptions() {
        List<Region> leafRegions = findLeafRegions();
        if (leafRegions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Region> regionMap = createRegionMap();
        return createRegionOptions(leafRegions, regionMap);
    }

    private Region findRegionInMap(String code, Map<String, Region> regionMap) {
        Region region = regionMap.get(code);
        if (region == null) {
            throw new jakarta.persistence.EntityNotFoundException("Region not found: " + code);
        }
        return region;
    }

    private List<RegionDto> buildHierarchyFromMap(Region region, Map<String, Region> regionMap) {
        List<RegionDto> result = new ArrayList<>();
        collectHierarchyFromMap(region, regionMap, result);
        Collections.reverse(result);
        return result;
    }

    private void collectHierarchyFromMap(Region region, Map<String, Region> regionMap, List<RegionDto> result) {
        Region current = region;
        while (current != null) {
            result.add(RegionDto.from(current));
            current = getParentFromMap(current, regionMap);
        }
    }

    private Region getParentFromMap(Region region, Map<String, Region> regionMap) {
        String parentCode = region.getParentCode();
        return parentCode != null ? regionMap.get(parentCode) : null;
    }

    private List<Region> findLeafRegions() {
        return regionRepository.findLeafRegionsOrderByCode();
    }

    private Map<String, Region> createRegionMap() {
        return regionRepository.findAll()
            .stream()
            .collect(Collectors.toMap(Region::getCode, region -> region));
    }

    private List<RegionOptionDto> createRegionOptions(List<Region> leafRegions, Map<String, Region> regionMap) {
        return leafRegions.stream()
            .map(leaf -> createRegionOption(leaf, regionMap))
            .collect(Collectors.toList());
    }

    private RegionOptionDto createRegionOption(Region leaf, Map<String, Region> regionMap) {
        List<String> hierarchyCodes = buildHierarchyPath(leaf, regionMap);
        String fullCode = String.join("-", hierarchyCodes);
        return new RegionOptionDto(leaf.getCode(), fullCode);
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
