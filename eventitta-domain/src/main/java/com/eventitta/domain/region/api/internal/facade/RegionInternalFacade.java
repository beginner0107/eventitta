package com.eventitta.domain.region.api.internal.facade;

import com.eventitta.domain.region.api.internal.view.RegionReferenceView;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface RegionInternalFacade {

    void ensureExists(String regionCode);

    Optional<RegionReferenceView> findByCode(String regionCode);

    Map<String, RegionReferenceView> findByCodes(Collection<String> regionCodes);
}
