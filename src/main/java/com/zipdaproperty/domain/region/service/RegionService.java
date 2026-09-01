package com.zipdaproperty.domain.region.service;

import com.zipdaproperty.domain.region.repository.RegionQueryDSLRepository;
import com.zipdaproperty.domain.region.response.RegionSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionQueryDSLRepository regionQueryDSLRepository;

    public List<RegionSummaryResponse> getRootRegions(){
        return regionQueryDSLRepository.findRootRegions();
    }
}
