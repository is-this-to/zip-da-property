package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.favorite.repository.PropertyFavoriteRepository;
import com.zipdaproperty.domain.option.response.PropertyDetailOptionResponse;
import com.zipdaproperty.domain.option.service.PropertyOptionQueryService;
import com.zipdaproperty.domain.property.repository.PropertyPublicDetailQueryRepository;
import com.zipdaproperty.domain.property.repository.PropertyPublicDetailQueryRow;
import com.zipdaproperty.domain.property.response.PropertyPublicDetailImageResponse;
import com.zipdaproperty.domain.property.response.PropertyPublicDetailResponse;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyPublicDetailService {

    private final PropertyPublicDetailQueryRepository queryRepository;
    private final PropertyOptionQueryService optionQueryService;
    private final PropertyPublicImageService imageService;
    private final PropertyFavoriteRepository favoriteRepository;

    public PropertyPublicDetailResponse findDetail(
            Long propertyId,
            Long memberId
    ) {
        if (propertyId == null || propertyId <= 0) {
            throw propertyNotFound();
        }

        PropertyPublicDetailQueryRow row = queryRepository
                .findPublicDetail(propertyId)
                .orElseThrow(this::propertyNotFound);
        List<PropertyDetailOptionResponse> options =
                optionQueryService.getDetailVisibleOptions(propertyId);
        List<PropertyPublicDetailImageResponse> images =
                imageService.findImages(propertyId);
        long favoriteCount = favoriteRepository.countByPropertyId(propertyId);
        boolean isFavorite = memberId != null
                && favoriteRepository
                .findByMemberIdAndPropertyId(memberId, propertyId)
                .isPresent();

        return PropertyPublicDetailResponse.from(
                row.property(),
                row.publicAddress(),
                row.publicLocation(),
                options,
                images,
                favoriteCount,
                isFavorite
        );
    }

    private BusinessException propertyNotFound() {
        return new BusinessException(
                CustomResponseCode.PROPERTY_NOT_FOUND,
                "공개 중인 매물을 찾을 수 없습니다."
        );
    }
}
