package com.zipdaproperty.domain.apartment.service;

import com.zipdaproperty.domain.apartment.repository.ApartmentComplexQueryDSLRepository;
import com.zipdaproperty.domain.apartment.request.ApartmentComplexSearchRequest;
import com.zipdaproperty.domain.apartment.response.ApartmentComplexSearchResponse;
import com.zipdaproperty.domain.apartment.response.ApartmentComplexSummaryResponse;
import com.zipdaproperty.domain.region.entity.Region;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApartmentComplexQueryServiceTest {

    private static final Long REGION_ID = 53390L;

    private final ApartmentComplexQueryDSLRepository apartmentComplexQueryDSLRepository =
            mock(ApartmentComplexQueryDSLRepository.class);
    private final RegionRepository regionRepository =
            mock(RegionRepository.class);
    private final ApartmentComplexQueryService service =
            new ApartmentComplexQueryService(
                    apartmentComplexQueryDSLRepository,
                    regionRepository
            );

    @Test
    void search_activeRegion_returnsLimitedItems() {
        Region region = mock(Region.class);
        ApartmentComplexSummaryResponse item = new ApartmentComplexSummaryResponse(
                15L,
                "래미안 역삼",
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2020, 3, 15)
        );
        when(regionRepository.findByRegionIdAndIsActiveTrue(REGION_ID))
                .thenReturn(Optional.of(region));
        when(apartmentComplexQueryDSLRepository.findActiveComplexes(
                REGION_ID,
                "래미안",
                10
        )).thenReturn(List.of(item));

        ApartmentComplexSearchResponse response = service.search(
                new ApartmentComplexSearchRequest(REGION_ID, " 래미안 ", 10)
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().complexName()).isEqualTo("래미안 역삼");

        verify(apartmentComplexQueryDSLRepository).findActiveComplexes(
                REGION_ID,
                "래미안",
                10
        );
    }

    @Test
    void search_unknownRegion_rejectsBeforeComplexQuery() {
        when(regionRepository.findByRegionIdAndIsActiveTrue(REGION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.search(
                new ApartmentComplexSearchRequest(REGION_ID, null, null)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.NOT_FOUND_RESOURCE)
        );

        verifyNoInteractions(apartmentComplexQueryDSLRepository);
    }
}
