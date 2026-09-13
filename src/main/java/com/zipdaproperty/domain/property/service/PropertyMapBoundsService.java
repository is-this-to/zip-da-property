package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PropertyMapResponseType;
import com.zipdaproperty.domain.property.model.PropertyMapBounds;
import com.zipdaproperty.domain.property.model.PropertyMapSearchCondition;
import com.zipdaproperty.domain.property.policy.PropertyMapZoomPolicy;
import com.zipdaproperty.domain.property.repository.PropertyMapBoundsQueryRepository;
import com.zipdaproperty.domain.property.repository.PropertyMapBoundsQueryRow;
import com.zipdaproperty.domain.property.repository.PropertyMapRegionAggregateQueryRepository;
import com.zipdaproperty.domain.property.repository.PropertyMapRegionAggregateQueryRow;
import com.zipdaproperty.domain.property.request.PropertyMapBoundsRequest;
import com.zipdaproperty.domain.property.response.PropertyMapBoundsItemResponse;
import com.zipdaproperty.domain.property.response.PropertyMapBoundsResponse;
import com.zipdaproperty.domain.property.response.PropertyMapRegionAggregateItemResponse;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyMapBoundsService {

    private static final int MAX_MAP_ITEMS = 500;

    private final PropertyMapBoundsQueryRepository
            propertyMapBoundsQueryRepository;

    private final PropertyMapRegionAggregateQueryRepository
            propertyMapRegionAggregateQueryRepository;

    private final PropertyMapZoomPolicy
            propertyMapZoomPolicy;

    @Transactional(readOnly = true)
    public PropertyMapBoundsResponse findProperties(
            PropertyMapBoundsRequest request
    ) {
        PropertyMapBounds bounds = request.toBounds();
        PropertyMapSearchCondition condition =
                request.toSearchCondition();

        PropertyMapResponseType responseType =
                propertyMapZoomPolicy.resolveResponseType(
                        bounds.zoomLevel()
                );

        if (responseType == PropertyMapResponseType.REGION_AGGREGATE) {
            return findRegionAggregates(
                    bounds,
                    condition
            );
        }

        return findPropertyItems(
                bounds,
                condition,
                responseType
        );
    }

    private PropertyMapBoundsResponse findRegionAggregates(
            PropertyMapBounds bounds,
            PropertyMapSearchCondition condition
    ) {
        int targetRegionLevel =
                propertyMapZoomPolicy.resolveAggregationRegionLevel(
                        bounds.zoomLevel()
                );

        List<PropertyMapRegionAggregateQueryRow> rows =
                propertyMapRegionAggregateQueryRepository.findRegionAggregates(
                        bounds,
                        condition,
                        targetRegionLevel
                );

        List<PropertyMapRegionAggregateItemResponse> items =
                rows.stream()
                        .map(this::toRegionAggregateResponse)
                        .toList();

        long totalCount = rows.stream()
                .mapToLong(PropertyMapRegionAggregateQueryRow::propertyCount)
                .sum();

        return PropertyMapBoundsResponse.of(
                PropertyMapResponseType.REGION_AGGREGATE,
                items,
                totalCount,
                false
        );
    }

    private PropertyMapBoundsResponse findPropertyItems(
            PropertyMapBounds bounds,
            PropertyMapSearchCondition condition,
            PropertyMapResponseType responseType
    ) {

        long totalCount =
                propertyMapBoundsQueryRepository
                        .countPublicPropertiesInBounds(
                                bounds,
                                condition
                        );

        if (totalCount == 0L) {
            return PropertyMapBoundsResponse.of(
                    responseType,
                    List.of(),
                    0L,
                    false
            );
        }

        List<PropertyMapBoundsQueryRow> rows =
                propertyMapBoundsQueryRepository
                        .findPublicPropertiesInBounds(
                                bounds,
                                condition,
                                MAX_MAP_ITEMS
                        );

        List<PropertyMapBoundsItemResponse> items =
                rows.stream()
                        .map(this::toItemResponse)
                        .toList();

        return PropertyMapBoundsResponse.of(
                responseType,
                items,
                totalCount,
                totalCount > MAX_MAP_ITEMS
        );
    }

    private PropertyMapRegionAggregateItemResponse toRegionAggregateResponse(
            PropertyMapRegionAggregateQueryRow row
    ) {
        return new PropertyMapRegionAggregateItemResponse(
                row.regionId(),
                row.regionName(),
                row.regionLevel(),
                row.regionType(),
                row.latitude(),
                row.longitude(),
                row.propertyCount()
        );
    }

    private PropertyMapBoundsItemResponse toItemResponse(
            PropertyMapBoundsQueryRow row
    ) {
        Point publicLocation =
                row.publicLocation();

        return new PropertyMapBoundsItemResponse(
                row.propertyId(),
                row.regionId(),
                row.title(),
                row.propertyType(),
                row.transactionType(),
                row.salePrice(),
                row.deposit(),
                row.monthlyRent(),
                row.publicAddress(),
                publicLocation.getY(),
                publicLocation.getX()
        );
    }
}
