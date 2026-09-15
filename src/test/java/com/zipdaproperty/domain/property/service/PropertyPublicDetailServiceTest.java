package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.favorite.entity.PropertyFavorite;
import com.zipdaproperty.domain.favorite.repository.PropertyFavoriteRepository;
import com.zipdaproperty.domain.option.response.PropertyDetailOptionResponse;
import com.zipdaproperty.domain.option.service.PropertyOptionQueryService;
import com.zipdaproperty.domain.option.type.OptionCategory;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyPublicDetailQueryRepository;
import com.zipdaproperty.domain.property.repository.PropertyPublicDetailQueryRow;
import com.zipdaproperty.domain.property.response.PropertyPublicDetailImageResponse;
import com.zipdaproperty.domain.property.response.PropertyPublicDetailResponse;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyPublicDetailServiceTest {

    private static final Long PROPERTY_ID = 884685586571263701L;
    private static final Long MEMBER_ID = 1001L;

    private final PropertyPublicDetailQueryRepository queryRepository =
            mock(PropertyPublicDetailQueryRepository.class);
    private final PropertyOptionQueryService optionQueryService =
            mock(PropertyOptionQueryService.class);
    private final PropertyPublicImageService imageService =
            mock(PropertyPublicImageService.class);
    private final PropertyFavoriteRepository favoriteRepository =
            mock(PropertyFavoriteRepository.class);
    private final PropertyPublicDetailService service =
            new PropertyPublicDetailService(
                    queryRepository,
                    optionQueryService,
                    imageService,
                    favoriteRepository
            );

    @Test
    void findDetail_loggedInMember_returnsPublicDataOptionsImagesAndFavoriteState() {
        Property property = property();
        Point publicLocation = new GeometryFactory()
                .createPoint(new Coordinate(127.01, 37.51));
        PropertyDetailOptionResponse option = new PropertyDetailOptionResponse(
                "PARKING", "주차", OptionCategory.LIVING, "true", 1
        );
        PropertyPublicDetailImageResponse image =
                new PropertyPublicDetailImageResponse(
                        101L, "https://example.test/image", 0, true
                );
        when(queryRepository.findPublicDetail(PROPERTY_ID)).thenReturn(Optional.of(
                new PropertyPublicDetailQueryRow(
                        property,
                        "서울특별시 강남구 역삼동",
                        publicLocation
                )
        ));
        when(optionQueryService.getDetailVisibleOptions(PROPERTY_ID))
                .thenReturn(List.of(option));
        when(imageService.findImages(PROPERTY_ID)).thenReturn(List.of(image));
        when(favoriteRepository.countByPropertyId(PROPERTY_ID)).thenReturn(7L);
        when(favoriteRepository.findByMemberIdAndPropertyId(MEMBER_ID, PROPERTY_ID))
                .thenReturn(Optional.of(mock(PropertyFavorite.class)));

        PropertyPublicDetailResponse response = service.findDetail(PROPERTY_ID, MEMBER_ID);

        assertThat(response.propertyId()).isEqualTo(PROPERTY_ID);
        assertThat(response.propertyType()).isEqualTo(PropertyType.APARTMENT);
        assertThat(response.publicAddress()).isEqualTo("서울특별시 강남구 역삼동");
        assertThat(response.latitude()).isEqualTo(37.51);
        assertThat(response.longitude()).isEqualTo(127.01);
        assertThat(response.options()).containsExactly(option);
        assertThat(response.images()).containsExactly(image);
        assertThat(response.favoriteCount()).isEqualTo(7L);
        assertThat(response.isFavorite()).isTrue();
    }

    @Test
    void findDetail_anonymousUser_returnsFalseWithoutFavoriteLookup() {
        PropertyPublicDetailQueryRow row = publicRow();
        when(queryRepository.findPublicDetail(PROPERTY_ID))
                .thenReturn(Optional.of(row));
        when(optionQueryService.getDetailVisibleOptions(PROPERTY_ID)).thenReturn(List.of());
        when(imageService.findImages(PROPERTY_ID)).thenReturn(List.of());
        when(favoriteRepository.countByPropertyId(PROPERTY_ID)).thenReturn(2L);

        PropertyPublicDetailResponse response = service.findDetail(PROPERTY_ID, null);

        assertThat(response.isFavorite()).isFalse();
        assertThat(response.favoriteCount()).isEqualTo(2L);
        verify(favoriteRepository, never())
                .findByMemberIdAndPropertyId(anyLong(), anyLong());
    }

    @Test
    void findDetail_nonPublicOrMissingProperty_throwsNotFoundBeforeRelatedQueries() {
        when(queryRepository.findPublicDetail(PROPERTY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findDetail(PROPERTY_ID, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.PROPERTY_NOT_FOUND));
        verifyNoInteractions(optionQueryService, imageService, favoriteRepository);
    }

    private PropertyPublicDetailQueryRow publicRow() {
        return new PropertyPublicDetailQueryRow(
                property(),
                "서울특별시 강남구 역삼동",
                new GeometryFactory().createPoint(new Coordinate(127.01, 37.51))
        );
    }

    private Property property() {
        Property property = mock(Property.class);
        when(property.getPropertyId()).thenReturn(PROPERTY_ID);
        when(property.getPropertyType()).thenReturn(PropertyType.APARTMENT);
        when(property.getTitle()).thenReturn("공개 매물");
        return property;
    }
}
