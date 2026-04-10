package com.eventitta.domain.region.repository;

import com.eventitta.domain.region.domain.Region;
import com.eventitta.domain.common.repository.BaseRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface RegionRepository extends BaseRepository<Region, String> {
}
