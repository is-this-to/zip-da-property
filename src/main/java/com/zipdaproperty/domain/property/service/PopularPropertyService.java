package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PopularPropertyRegion;
import com.zipdaproperty.domain.property.repository.PopularPropertyQueryRepository;
import com.zipdaproperty.domain.property.repository.PopularPropertyQueryRow;
import com.zipdaproperty.domain.property.response.PopularPropertyItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
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
        Map<Long, String> imageUrls = imageService.findRepresentativeImageUrls(
                rows.stream().map(PopularPropertyQueryRow::propertyId).toList()
        );

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
}
