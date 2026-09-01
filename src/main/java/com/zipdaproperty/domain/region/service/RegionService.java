package com.zipdaproperty.domain.region.service;

import com.zipdaproperty.domain.region.repository.RegionQueryDSLRepository;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.domain.region.response.RegionSummaryResponse;
import com.zipdaproperty.global.error.custom.business.NotFoundResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionQueryDSLRepository regionQueryDSLRepository;
    private final RegionRepository regionRepository;

    public List<RegionSummaryResponse> getRootRegions(){
        return regionQueryDSLRepository.findRootRegions();
    }

    public List<RegionSummaryResponse> getChildRegions(
            Long parentRegionId
    ){
        regionRepository
                .findByRegionIdAndIsActiveTrue(parentRegionId)
                .orElseThrow(() -> new NotFoundResourceException(
                        "활성 상태의 부모 지역을 찾을 수 없습니다. parentRegionId = " + parentRegionId
                ));
        return regionQueryDSLRepository.findChildRegions(parentRegionId);
    }
}
