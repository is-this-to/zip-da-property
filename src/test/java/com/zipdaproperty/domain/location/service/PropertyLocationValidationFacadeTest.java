package com.zipdaproperty.domain.location.service;

import com.zipdaproperty.domain.location.command.PropertyLocationValidationCommand;
import com.zipdaproperty.domain.location.model.VerifiedPropertyLocation;
import com.zipdaproperty.domain.location.request.PropertyLocationValidationRequest;
import com.zipdaproperty.domain.location.response.PropertyLocationValidationResponse;
import com.zipdaproperty.domain.region.entity.Region;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyLocationValidationFacadeTest {

    private static final Long REGION_ID = 53390L;

    private final PropertyRegionValidationService propertyRegionValidationService =
            mock(PropertyRegionValidationService.class);
    private final RegionRepository regionRepository = mock(RegionRepository.class);
    private final PropertyLocationValidationFacade facade =
            new PropertyLocationValidationFacade(
                    propertyRegionValidationService,
                    regionRepository
            );

    @Test
    void validate_validAddress_returnsRegionAndNormalizedAddress() {
        BigDecimal longitude = new BigDecimal("127.012345");
        BigDecimal latitude = new BigDecimal("37.512345");
        PropertyLocationValidationRequest request =
                new PropertyLocationValidationRequest(
                        " 서울특별시  강남구 테헤란로 123 ",
                        null,
                        "1168010100",
                        longitude,
                        latitude
                );

        GeometryFactory geometryFactory =
                new GeometryFactory(new PrecisionModel(), 4326);
        VerifiedPropertyLocation verifiedLocation =
                new VerifiedPropertyLocation(
                        REGION_ID,
                        "1168010100",
                        geometryFactory.createPoint(
                                new Coordinate(
                                        longitude.doubleValue(),
                                        latitude.doubleValue()
                                )
                        )
                );
        Region region = mock(Region.class);

        when(propertyRegionValidationService.validate(
                org.mockito.ArgumentMatchers.any(PropertyLocationValidationCommand.class)
        )).thenReturn(verifiedLocation);
        when(regionRepository.findByRegionIdAndIsActiveTrue(REGION_ID))
                .thenReturn(Optional.of(region));
        when(region.getRegionCode()).thenReturn("1168010100");
        when(region.getRegionName()).thenReturn("역삼동");
        when(region.getFullRegionName()).thenReturn("서울특별시 강남구 역삼동");

        PropertyLocationValidationResponse response = facade.validate(request);

        assertThat(response.regionId()).isEqualTo(REGION_ID);
        assertThat(response.roadAddress())
                .isEqualTo("서울특별시 강남구 테헤란로 123");
        assertThat(response.fullRegionName())
                .isEqualTo("서울특별시 강남구 역삼동");
        verify(propertyRegionValidationService).validate(argThat(command ->
                command.legalDongCode().equals("1168010100")
                        && command.longitude().compareTo(longitude) == 0
                        && command.latitude().compareTo(latitude) == 0
        ));
    }
}
