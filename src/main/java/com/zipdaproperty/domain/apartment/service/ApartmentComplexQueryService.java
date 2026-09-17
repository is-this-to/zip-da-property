package com.zipdaproperty.domain.apartment.service;

import com.zipdaproperty.domain.apartment.repository.ApartmentComplexQueryDSLRepository;
import com.zipdaproperty.domain.apartment.request.ApartmentComplexSearchRequest;
import com.zipdaproperty.domain.apartment.response.ApartmentComplexSearchResponse;
import com.zipdaproperty.domain.apartment.response.ApartmentComplexSummaryResponse;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.global.error.custom.business.NotFoundResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApartmentComplexQueryService {

    private final ApartmentComplexQueryDSLRepository apartmentComplexQueryDSLRepository;
    private final RegionRepository regionRepository;

    public ApartmentComplexSearchResponse search(ApartmentComplexSearchRequest request) {
        validateRegion(request.regionId());

        List<ApartmentComplexSummaryResponse> items = apartmentComplexQueryDSLRepository
                .findActiveComplexes(
                        request.regionId(),
                        request.keyword(),
                        request.size()
                );

        return new ApartmentComplexSearchResponse(items);
    }

    private void validateRegion(Long regionId) {
        regionRepository
                .findByRegionIdAndIsActiveTrue(regionId)
                .orElseThrow(() -> new NotFoundResourceException(
                        "활성 상태의 Region을 찾을 수 없습니다. regionId = " + regionId
                ));
    }
}
