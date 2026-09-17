package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PopularPropertyRegion;
import com.zipdaproperty.domain.property.repository.PopularPropertyQueryRepository;
import com.zipdaproperty.domain.property.repository.PopularPropertyQueryRow;
import com.zipdaproperty.domain.property.response.PopularPropertyItemResponse;
import com.zipdaproperty.global.error.custom.business.FileManagedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularPropertyService {

    private final PopularPropertyQueryRepository queryRepository;
    private final PropertyPublicImageService imageService;

    @Transactional(readOnly = true)
    public List<PopularPropertyItemResponse> findPopularProperties(
            PopularPropertyRegion region,
            int size
    ) {
        List<PopularPropertyQueryRow> rows = queryRepository.findPopularProperties(
                region,
                size
        );
        Map<Long, String> imageUrls = findRepresentativeImageUrls(rows);

        return rows.stream()
                .map(row -> new PopularPropertyItemResponse(
                        row.propertyId(),
                        row.title(),
                        row.propertyType(),
                        row.transactionType(),
                        row.salePrice(),
                        row.deposit(),
                        row.monthlyRent(),
                        row.exclusiveArea(),
                        row.locationSummary(),
                        imageUrls.get(row.propertyId()),
                        row.favoriteCount()
                ))
                .toList();
    }

    private Map<Long, String> findRepresentativeImageUrls(
            List<PopularPropertyQueryRow> rows
    ) {
        try {
            return imageService.findRepresentativeImageUrls(
                    rows.stream()
                            .map(PopularPropertyQueryRow::propertyId)
                            .toList()
            );
        } catch (FileManagedException exception) {
            log.warn(
                    "인기 매물 대표 이미지 URL 생성에 실패하여 기본 이미지로 응답합니다. propertyIds={}",
                    rows.stream()
                            .map(PopularPropertyQueryRow::propertyId)
                            .toList()
            );
            return Map.of();
        }
    }
}
