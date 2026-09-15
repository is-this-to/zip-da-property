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
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyAddressServiceTest {

    private static final Long PROPERTY_ID = 884700000000000001L;
    private static final Long REGION_ID = 53390L;
    private static final String LEGAL_DONG_CODE = "2726010100";

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final PropertyRegionValidationService validationService =
            mock(PropertyRegionValidationService.class);

    private final PropertyPublicLocationGenerator locationGenerator =
            mock(PropertyPublicLocationGenerator.class);

    private final PropertyAddressRepository addressRepository =
            mock(PropertyAddressRepository.class);

    private final RegionRepository regionRepository =
            mock(RegionRepository.class);

    private final PropertyAddressService addressService =
            new PropertyAddressService(
                    validationService,
                    locationGenerator,
                    new PropertyAddressNormalizer(),
                    addressRepository,
                    regionRepository
            );

    private final ActorContext actorContext = ActorContext.member(
            1001L,
            ActorRole.USER,
            "property-address-test"
    );

    @Test
    void prepare_validKakaoResultReturnsVerifiedAndPublicLocation() {
        Point exactLocation = point(128.625123, 35.859321);
        Point publicLocation = point(128.626000, 35.860000);

        when(validationService.validate(any()))
                .thenReturn(
                        new VerifiedPropertyLocation(
                                REGION_ID,
                                LEGAL_DONG_CODE,
                                exactLocation
                        )
                );

        when(locationGenerator.generate(any()))
                .thenReturn(
                        new GeneratedPublicLocation(publicLocation)
                );

        Region region = mock(Region.class);
        when(region.getFullRegionName())
                .thenReturn("대구광역시 수성구 범어동");
        when(regionRepository.findByRegionIdAndIsActiveTrue(REGION_ID))
                .thenReturn(Optional.of(region));

        PreparedPropertyAddress preparedAddress =
                addressService.prepare(
                        PROPERTY_ID,
                        new PropertyAddressCommand(
                                " 대구 수성구   달구벌대로 2450 ",
                                "대구광역시 수성구 범어동 123",
                                LEGAL_DONG_CODE,
                                new BigDecimal("128.625123"),
                                new BigDecimal("35.859321")
                        )
                );

        assertThat(preparedAddress.regionId()).isEqualTo(REGION_ID);
        assertThat(preparedAddress.roadAddress())
                .isEqualTo("대구 수성구 달구벌대로 2450");
        assertThat(preparedAddress.publicAddress())
                .isEqualTo("대구광역시 수성구 범어동");
        assertThat(preparedAddress.exactLocation())
                .isEqualTo(exactLocation);
        assertThat(preparedAddress.publicLocation())
                .isEqualTo(publicLocation);

        ArgumentCaptor<PropertyLocationValidationCommand>
                validationCommandCaptor =
                ArgumentCaptor.forClass(
                        PropertyLocationValidationCommand.class
                );

        verify(validationService)
                .validate(validationCommandCaptor.capture());

        assertThat(validationCommandCaptor.getValue().legalDongCode())
                .isEqualTo(LEGAL_DONG_CODE);

        ArgumentCaptor<PublicLocationGenerationCommand>
                generationCommandCaptor =
                ArgumentCaptor.forClass(
                        PublicLocationGenerationCommand.class
                );

        verify(locationGenerator)
                .generate(generationCommandCaptor.capture());

        assertThat(generationCommandCaptor.getValue().propertyId())
                .isEqualTo(PROPERTY_ID);
        assertThat(
                generationCommandCaptor
                        .getValue()
                        .normalizedAddressHash()
        ).matches("^[0-9a-f]{64}$");
    }

    @Test
    void create_savesOneToOnePropertyAddressWithoutDetailAddress() {
        Property property = mock(Property.class);
        PreparedPropertyAddress preparedAddress = preparedAddress();

        addressService.create(
                property,
                preparedAddress,
                actorContext
        );

        ArgumentCaptor<PropertyAddress> captor =
                ArgumentCaptor.forClass(PropertyAddress.class);

        verify(addressRepository).save(captor.capture());

        PropertyAddress savedAddress = captor.getValue();
        assertThat(savedAddress.getProperty()).isSameAs(property);
        assertThat(savedAddress.getLegalDongCode())
                .isEqualTo(LEGAL_DONG_CODE);
        assertThat(savedAddress.getDetailAddressEncrypted()).isNull();
        assertThat(savedAddress.getDetailAddressKeyVersion()).isNull();
        assertThat(savedAddress.getDisclosureLevel())
                .isEqualTo(DisclosureLevel.APPROXIMATE);
        assertThat(savedAddress.getLocationSource())
                .isEqualTo(LocationSource.KAKAO_LOCAL);
    }

    @Test
    void change_updatesExistingAddressInsteadOfAddingAnotherRow() {
        Property property = mock(Property.class);
        when(property.getPropertyId()).thenReturn(PROPERTY_ID);

        PropertyAddress existingAddress = PropertyAddress.create(
                property,
                LEGAL_DONG_CODE,
                "기존 도로명 주소",
                "기존 지번 주소",
                null,
                null,
                point(128.620000, 35.850000),
                "대구광역시 수성구 범어동",
                point(128.621000, 35.851000),
                DisclosureLevel.APPROXIMATE,
                Instant.parse("2026-09-10T00:00:00Z"),
                LocationSource.KAKAO_LOCAL,
                actorContext
        );

        when(
                addressRepository
                        .findByProperty_PropertyIdAndDeletedAtIsNull(
                                PROPERTY_ID
                        )
        ).thenReturn(Optional.of(existingAddress));

        PreparedPropertyAddress preparedAddress = preparedAddress();

        addressService.change(
                property,
                preparedAddress,
                actorContext
        );

        assertThat(existingAddress.getExactRoadAddress())
                .isEqualTo(preparedAddress.roadAddress());
        assertThat(existingAddress.getExactLocation())
                .isEqualTo(preparedAddress.exactLocation());
        assertThat(existingAddress.getPublicLocation())
                .isEqualTo(preparedAddress.publicLocation());
    }

    private PreparedPropertyAddress preparedAddress() {
        return new PreparedPropertyAddress(
                REGION_ID,
                LEGAL_DONG_CODE,
                "대구 수성구 달구벌대로 2450",
                "대구광역시 수성구 범어동 123",
                point(128.625123, 35.859321),
                "대구광역시 수성구 범어동",
                point(128.626000, 35.860000),
                Instant.parse("2026-09-11T00:00:00Z")
        );
    }

    private static Point point(
            double longitude,
            double latitude
    ) {
        Point point = GEOMETRY_FACTORY.createPoint(
                new Coordinate(longitude, latitude)
        );
        point.setSRID(4326);
        return point;
    }
}
