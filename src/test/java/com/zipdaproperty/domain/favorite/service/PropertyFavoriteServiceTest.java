package com.zipdaproperty.domain.favorite.service;

import com.zipdaproperty.domain.favorite.repository.PropertyFavoriteListQueryRepository;
import com.zipdaproperty.domain.favorite.repository.PropertyFavoriteListQueryRow;
import com.zipdaproperty.domain.favorite.response.PropertyFavoriteListResponse;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.service.PropertyPublicImageService;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyFavoriteServiceTest {

    private static final Long PROPERTY_ID = 884685586571263701L;
    private static final ActorContext ACTOR_CONTEXT = ActorContext.member(
            1001L,
            ActorRole.USER,
            "favorite-list-test"
    );

    private final PropertyFavoriteCommandService commandService =
            mock(PropertyFavoriteCommandService.class);
    private final PropertyFavoriteListQueryRepository listQueryRepository =
            mock(PropertyFavoriteListQueryRepository.class);
    private final PropertyPublicImageService imageService =
            mock(PropertyPublicImageService.class);
    private final PropertyFavoriteService service = new PropertyFavoriteService(
            commandService,
            listQueryRepository,
            imageService
    );

    @Test
    void getMyFavorites_connectsRepresentativePresignedImageUrl() {
        when(listQueryRepository.findMyFavorites(1001L, 0L, 21))
                .thenReturn(List.of(new PropertyFavoriteListQueryRow(
                        PROPERTY_ID,
                        PropertyType.APARTMENT,
                        TransactionType.SALE,
                        500_000_000L,
                        null,
                        null,
                        new BigDecimal("84.50"),
                        "서울특별시 강남구",
                        10,
                        PublisherType.DIRECT_OWNER,
                        7L
                )));
        when(imageService.findRepresentativeImageUrls(List.of(PROPERTY_ID)))
                .thenReturn(Map.of(
                        PROPERTY_ID,
                        "https://example.test/favorite-representative"
                ));

        PropertyFavoriteListResponse response = service.getMyFavorites(
                ACTOR_CONTEXT,
                0,
                20
        );

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.propertyId()).isEqualTo(PROPERTY_ID);
            assertThat(item.representativeImageUrl())
                    .isEqualTo("https://example.test/favorite-representative");
        });
    }
}
