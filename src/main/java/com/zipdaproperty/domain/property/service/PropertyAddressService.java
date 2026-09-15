package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.location.command.PropertyLocationValidationCommand;
import com.zipdaproperty.domain.location.command.PublicLocationGenerationCommand;
import com.zipdaproperty.domain.location.model.GeneratedPublicLocation;
import com.zipdaproperty.domain.location.model.VerifiedPropertyLocation;
import com.zipdaproperty.domain.location.service.PropertyPublicLocationGenerator;
import com.zipdaproperty.domain.location.service.PropertyRegionValidationService;
import com.zipdaproperty.domain.property.command.PropertyAddressCommand;
import com.zipdaproperty.domain.property.constant.DisclosureLevel;
import com.zipdaproperty.domain.property.constant.LocationSource;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyAddress;
import com.zipdaproperty.domain.property.model.PreparedPropertyAddress;
import com.zipdaproperty.domain.property.repository.PropertyAddressRepository;
import com.zipdaproperty.domain.region.entity.Region;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PropertyAddressService {

    private final PropertyRegionValidationService
            propertyRegionValidationService;

    private final PropertyPublicLocationGenerator
            propertyPublicLocationGenerator;

    private final PropertyAddressNormalizer
            propertyAddressNormalizer;

    private final PropertyAddressRepository
            propertyAddressRepository;

    private final RegionRepository regionRepository;

    public PreparedPropertyAddress prepare(
            Long propertyId,
            PropertyAddressCommand command
    ) {
        if (command == null) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "매물 주소는 필수입니다."
            );
        }

        String roadAddress =
                propertyAddressNormalizer.normalizeNullable(
                        command.roadAddress()
                );

        String jibunAddress =
                propertyAddressNormalizer.normalizeNullable(
                        command.jibunAddress()
                );

        String normalizedAddressHash =
                propertyAddressNormalizer
                        .createNormalizedAddressHash(
                                roadAddress,
                                jibunAddress
                        );

        VerifiedPropertyLocation verifiedLocation =
                propertyRegionValidationService.validate(
                        new PropertyLocationValidationCommand(
                                command.legalDongCode(),
                                command.longitude(),
                                command.latitude()
                        )
                );

        GeneratedPublicLocation generatedPublicLocation =
                propertyPublicLocationGenerator.generate(
                        new PublicLocationGenerationCommand(
                                propertyId,
                                normalizedAddressHash,
                                verifiedLocation
                        )
                );

        Region region = regionRepository
                .findByRegionIdAndIsActiveTrue(
                        verifiedLocation.regionId()
                )
                .orElseThrow(
                        () -> new BusinessException(
                                CustomResponseCode.NOT_FOUND_RESOURCE,
                                "검증된 활성 Region을 찾을 수 없습니다."
                        )
                );

        return new PreparedPropertyAddress(
                verifiedLocation.regionId(),
                verifiedLocation.legalDongCode(),
                roadAddress,
                jibunAddress,
                verifiedLocation.exactLocation(),
                region.getFullRegionName(),
                generatedPublicLocation.publicLocation(),
                Instant.now()
        );
    }

    public void create(
            Property property,
            PreparedPropertyAddress preparedAddress,
            ActorContext actorContext
    ) {
        PropertyAddress propertyAddress = PropertyAddress.create(
                property,
                preparedAddress.legalDongCode(),
                preparedAddress.roadAddress(),
                preparedAddress.jibunAddress(),
                null,
                null,
                preparedAddress.exactLocation(),
                preparedAddress.publicAddress(),
                preparedAddress.publicLocation(),
                DisclosureLevel.APPROXIMATE,
                preparedAddress.locationVerifiedAt(),
                LocationSource.KAKAO_LOCAL,
                actorContext
        );

        propertyAddressRepository.save(propertyAddress);
    }

    public void change(
            Property property,
            PreparedPropertyAddress preparedAddress,
            ActorContext actorContext
    ) {
        PropertyAddress propertyAddress =
                propertyAddressRepository
                        .findByProperty_PropertyIdAndDeletedAtIsNull(
                                property.getPropertyId()
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        CustomResponseCode.NOT_FOUND_RESOURCE,
                                        "수정할 매물 주소를 찾을 수 없습니다."
                                )
                        );

        propertyAddress.changeAddress(
                preparedAddress.legalDongCode(),
                preparedAddress.roadAddress(),
                preparedAddress.jibunAddress(),
                null,
                null,
                preparedAddress.exactLocation(),
                preparedAddress.publicAddress(),
                preparedAddress.publicLocation(),
                DisclosureLevel.APPROXIMATE,
                preparedAddress.locationVerifiedAt(),
                LocationSource.KAKAO_LOCAL,
                actorContext
        );
    }
}
