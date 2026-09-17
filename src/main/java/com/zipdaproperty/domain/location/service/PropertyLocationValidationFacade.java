package com.zipdaproperty.domain.location.service;

import com.zipdaproperty.domain.location.command.PropertyLocationValidationCommand;
import com.zipdaproperty.domain.location.model.VerifiedPropertyLocation;
import com.zipdaproperty.domain.location.request.PropertyLocationValidationRequest;
import com.zipdaproperty.domain.location.response.PropertyLocationValidationResponse;
import com.zipdaproperty.domain.region.entity.Region;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.global.error.custom.business.NotFoundResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyLocationValidationFacade {

    private final PropertyRegionValidationService propertyRegionValidationService;
    private final RegionRepository regionRepository;

    public PropertyLocationValidationResponse validate(
            PropertyLocationValidationRequest request
    ) {
        VerifiedPropertyLocation verifiedLocation = propertyRegionValidationService.validate(
                new PropertyLocationValidationCommand(
                        request.legalDongCode(),
                        request.longitude(),
                        request.latitude()
                )
        );

        Region region = regionRepository
                .findByRegionIdAndIsActiveTrue(verifiedLocation.regionId())
                .orElseThrow(() -> new NotFoundResourceException(
                        "검증된 활성 Region을 찾을 수 없습니다."
                ));

        return PropertyLocationValidationResponse.of(
                request,
                verifiedLocation,
                region
        );
    }
}
