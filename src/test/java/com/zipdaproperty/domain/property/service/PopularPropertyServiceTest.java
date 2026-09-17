package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PopularPropertyRegion;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.repository.PopularPropertyQueryRepository;
import com.zipdaproperty.domain.property.repository.PopularPropertyQueryRow;
import com.zipdaproperty.domain.property.response.PopularPropertyItemResponse;
import com.zipdaproperty.global.error.custom.business.FileManagedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PopularPropertyServiceTest {

    private static final Long PROPERTY_ID = 888043238994063385L;

    private final PopularPropertyQueryRepository queryRepository =
            mock(PopularPropertyQueryRepository.class);
    private final PropertyPublicImageService imageService =
            mock(PropertyPublicImageService.class);
    private final PopularPropertyService service =
            new PopularPropertyService(queryRepository, imageService);

    @Test
    void findPopularProperties_imageUrlGenerationFails_returnsPropertyWithoutImage() {
        PopularPropertyQueryRow row = new PopularPropertyQueryRow(
                PROPERTY_ID,
                "대구 인기 매물",
                PropertyType.APARTMENT,
                TransactionType.SALE,
                50000L,
                null,
                null,
                new BigDecimal("84.50"),
                "대구광역시 수성구",
                10L
        );
        when(queryRepository.findPopularProperties(PopularPropertyRegion.ALL, 5))
                .thenReturn(List.of(row));
        when(imageService.findRepresentativeImageUrls(List.of(PROPERTY_ID)))
                .thenThrow(new FileManagedException("이미지 조회 URL 생성에 실패했습니다."));

        List<PopularPropertyItemResponse> result = service.findPopularProperties(
                PopularPropertyRegion.ALL,
                5
        );

        assertThat(result).containsExactly(new PopularPropertyItemResponse(
                PROPERTY_ID,
                "대구 인기 매물",
                PropertyType.APARTMENT,
                TransactionType.SALE,
                50000L,
                null,
                null,
                new BigDecimal("84.50"),
                "대구광역시 수성구",
                null,
                10L
        ));
    }
}
