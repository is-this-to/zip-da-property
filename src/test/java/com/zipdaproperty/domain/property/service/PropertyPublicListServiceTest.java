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
import com.zipdaproperty.domain.favorite.repository.PropertyFavoriteListQueryRepository;
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
import java.util.Map;
import java.util.Set;

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
    @Mock PropertyPublicImageService imageService;
    @Mock PropertyFavoriteListQueryRepository favoriteQueryRepository;

    private PropertyPublicListService service;

    @BeforeEach
    void setUp() {
        service = new PropertyPublicListService(
                repository,
                codec,
                hasher,
                imageService,
                favoriteQueryRepository
        );
    }

    @Test
    void requestSizePlusOneAndReturnNextCursor() {
        PropertyPublicListRequest request = request(2);
        when(hasher.hash(any(), any())).thenReturn("context");
        when(codec.decode(null, PropertyMapSort.LATEST, "context")).thenReturn(null);
        when(repository.findPublicProperties(any(), any(), eq(null), eq(3)))
                .thenReturn(List.of(row(3L), row(2L), row(1L)));
        when(codec.encode(any(PropertyPublicListCursor.class))).thenReturn("next");

        when(imageService.findRepresentativeImageUrls(List.of(3L, 2L)))
                .thenReturn(Map.of(3L, "https://example.test/3"));
        when(favoriteQueryRepository.countByPropertyIds(List.of(3L, 2L)))
                .thenReturn(Map.of(3L, 7L, 2L, 2L));
        when(favoriteQueryRepository.findFavoritePropertyIds(
                1001L,
                List.of(3L, 2L)
        )).thenReturn(Set.of(3L));

        PropertyPublicListResponse response = service.findProperties(request, 1001L);

        assertThat(response.items()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("next");
        assertThat(response.items().getFirst().latitude()).isEqualTo(37.5);
        assertThat(response.items().getFirst().longitude()).isEqualTo(127.0);
        assertThat(response.items().getFirst().representativeImageUrl())
                .isEqualTo("https://example.test/3");
        assertThat(response.items().getFirst().favoriteCount()).isEqualTo(7L);
        assertThat(response.items().getFirst().isFavorite()).isTrue();
        assertThat(response.items().get(1).representativeImageUrl()).isNull();
        assertThat(response.items().get(1).favoriteCount()).isEqualTo(2L);
        assertThat(response.items().get(1).isFavorite()).isFalse();
        verify(repository).findPublicProperties(any(), any(), eq(null), eq(3));
    }

    @Test
    void lastPageHasNoNextCursor() {
        PropertyPublicListRequest request = request(2);
        when(hasher.hash(any(), any())).thenReturn("context");
        when(codec.decode(null, PropertyMapSort.LATEST, "context")).thenReturn(null);
        when(repository.findPublicProperties(any(), any(), eq(null), eq(3)))
                .thenReturn(List.of(row(1L)));

        when(imageService.findRepresentativeImageUrls(List.of(1L)))
                .thenReturn(Map.of());
        when(favoriteQueryRepository.countByPropertyIds(List.of(1L)))
                .thenReturn(Map.of());

        PropertyPublicListResponse response = service.findProperties(request, null);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.items().getFirst().favoriteCount()).isZero();
        assertThat(response.items().getFirst().isFavorite()).isFalse();
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
