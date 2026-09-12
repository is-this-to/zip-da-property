package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PropertyMapResponseType;
import com.zipdaproperty.domain.property.constant.PropertyMapSort;
import com.zipdaproperty.domain.property.model.PropertyMapBounds;
import com.zipdaproperty.domain.property.model.PropertyMapSearchCondition;
import com.zipdaproperty.domain.property.policy.PropertyMapZoomPolicy;
import com.zipdaproperty.domain.property.repository.PropertyMapBoundsQueryRepository;
import com.zipdaproperty.domain.property.repository.PropertyMapRegionAggregateQueryRepository;
import com.zipdaproperty.domain.property.request.PropertyMapBoundsRequest;
import com.zipdaproperty.domain.property.response.PropertyMapBoundsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyMapBoundsServiceTest {

    @Mock
    private PropertyMapBoundsQueryRepository boundsQueryRepository;

    @Mock
    private PropertyMapRegionAggregateQueryRepository
            regionAggregateQueryRepository;

    @Mock
    private PropertyMapZoomPolicy zoomPolicy;

    private PropertyMapBoundsService service;

    @BeforeEach
    void setUp() {
        service = new PropertyMapBoundsService(
                boundsQueryRepository,
                regionAggregateQueryRepository,
                zoomPolicy
        );
    }

    @Test
    @DisplayName("지역 집계 조회에 정규화한 필터 조건을 전달한다")
    void passConditionToRegionAggregateQuery() {
        PropertyMapBoundsRequest request = request(13);
        PropertyMapSearchCondition expectedCondition =
                request.toSearchCondition();

        when(zoomPolicy.resolveResponseType(13))
                .thenReturn(
                        PropertyMapResponseType.REGION_AGGREGATE
                );
        when(zoomPolicy.resolveAggregationRegionLevel(13))
                .thenReturn(1);
        when(regionAggregateQueryRepository.findRegionAggregates(
                any(PropertyMapBounds.class),
                eq(expectedCondition),
                eq(1)
        )).thenReturn(List.of());

        PropertyMapBoundsResponse response =
                service.findProperties(request);

        assertThat(response.responseType())
                .isEqualTo(
                        PropertyMapResponseType.REGION_AGGREGATE
                );
        assertThat(response.items()).isEmpty();
        assertThat(response.totalCount()).isZero();
        assertThat(response.truncated()).isFalse();
    }

    @Test
    @DisplayName("필터 적용 결과가 500개를 초과하면 truncated를 반환한다")
    void returnTruncatedWhenFilteredCountExceedsLimit() {
        PropertyMapBoundsRequest request = request(6);

        when(zoomPolicy.resolveResponseType(6))
                .thenReturn(
                        PropertyMapResponseType.PROPERTY_POINTS
                );
        when(boundsQueryRepository.countPublicPropertiesInBounds(
                any(PropertyMapBounds.class),
                any(PropertyMapSearchCondition.class)
        )).thenReturn(501L);
        when(boundsQueryRepository.findPublicPropertiesInBounds(
                any(PropertyMapBounds.class),
                any(PropertyMapSearchCondition.class),
                eq(500)
        )).thenReturn(List.of());

        PropertyMapBoundsResponse response =
                service.findProperties(request);

        assertThat(response.totalCount()).isEqualTo(501L);
        assertThat(response.truncated()).isTrue();
        assertThat(response.responseType())
                .isEqualTo(
                        PropertyMapResponseType.PROPERTY_POINTS
                );
    }

    @Test
    @DisplayName("필터 적용 결과가 없으면 목록 조회를 생략한다")
    void skipItemQueryWhenFilteredCountIsZero() {
        PropertyMapBoundsRequest request = request(3);

        when(zoomPolicy.resolveResponseType(3))
                .thenReturn(
                        PropertyMapResponseType.PROPERTY_MARKER
                );
        when(boundsQueryRepository.countPublicPropertiesInBounds(
                any(PropertyMapBounds.class),
                any(PropertyMapSearchCondition.class)
        )).thenReturn(0L);

        PropertyMapBoundsResponse response =
                service.findProperties(request);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalCount()).isZero();
        assertThat(response.truncated()).isFalse();

        verify(boundsQueryRepository, never())
                .findPublicPropertiesInBounds(
                        any(),
                        any(),
                        anyInt()
                );
    }

    private PropertyMapBoundsRequest request(int zoomLevel) {
        return new PropertyMapBoundsRequest(
                37.45,
                37.55,
                126.95,
                127.10,
                zoomLevel,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PropertyMapSort.LATEST
        );
    }
}
