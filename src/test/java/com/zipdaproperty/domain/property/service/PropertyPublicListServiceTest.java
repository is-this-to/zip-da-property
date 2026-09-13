package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PropertyMapSort;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.model.PropertyPublicListCursor;
import com.zipdaproperty.domain.property.repository.PropertyPublicListQueryRepository;
import com.zipdaproperty.domain.property.repository.PropertyPublicListQueryRow;
import com.zipdaproperty.domain.property.request.PropertyPublicListRequest;
import com.zipdaproperty.domain.property.response.PropertyPublicListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyPublicListServiceTest {

    @Mock PropertyPublicListQueryRepository repository;
    @Mock PropertyPublicListCursorCodec codec;
    @Mock PropertyListSearchContextHasher hasher;

    private PropertyPublicListService service;

    @BeforeEach
    void setUp() {
        service = new PropertyPublicListService(repository, codec, hasher);
    }

    @Test
    void requestSizePlusOneAndReturnNextCursor() {
        PropertyPublicListRequest request = request(2);
        when(hasher.hash(any(), any())).thenReturn("context");
        when(codec.decode(null, PropertyMapSort.LATEST, "context")).thenReturn(null);
        when(repository.findPublicProperties(any(), any(), eq(null), eq(3)))
                .thenReturn(List.of(row(3L), row(2L), row(1L)));
        when(codec.encode(any(PropertyPublicListCursor.class))).thenReturn("next");

        PropertyPublicListResponse response = service.findProperties(request);

        assertThat(response.items()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("next");
        assertThat(response.items().getFirst().latitude()).isEqualTo(37.5);
        assertThat(response.items().getFirst().longitude()).isEqualTo(127.0);
        verify(repository).findPublicProperties(any(), any(), eq(null), eq(3));
    }

    @Test
    void lastPageHasNoNextCursor() {
        PropertyPublicListRequest request = request(2);
        when(hasher.hash(any(), any())).thenReturn("context");
        when(codec.decode(null, PropertyMapSort.LATEST, "context")).thenReturn(null);
        when(repository.findPublicProperties(any(), any(), eq(null), eq(3)))
                .thenReturn(List.of(row(1L)));

        PropertyPublicListResponse response = service.findProperties(request);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    private PropertyPublicListRequest request(int size) {
        return new PropertyPublicListRequest(
                37.4, 37.6, 126.8, 127.2,
                null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                PropertyMapSort.LATEST, null, size
        );
    }

    private PropertyPublicListQueryRow row(long propertyId) {
        Point point = new GeometryFactory().createPoint(new Coordinate(127.0, 37.5));
        point.setSRID(4326);
        return new PropertyPublicListQueryRow(
                propertyId,
                10L,
                "테스트 매물",
                PropertyType.APARTMENT,
                TransactionType.SALE,
                500_000_000L,
                null,
                null,
                100_000L,
                new BigDecimal("84.50"),
                3,
                PublisherType.DIRECT_OWNER,
                "서울특별시",
                point,
                Instant.parse("2026-09-13T00:00:00Z"),
                500_000_000L
        );
    }
}
