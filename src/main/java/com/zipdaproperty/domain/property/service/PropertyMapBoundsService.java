package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.model.PropertyMapBounds;
import com.zipdaproperty.domain.property.repository.PropertyMapBoundsQueryRepository;
import com.zipdaproperty.domain.property.repository.PropertyMapBoundsQueryRow;
import com.zipdaproperty.domain.property.request.PropertyMapBoundsRequest;
import com.zipdaproperty.domain.property.response.PropertyMapBoundsItemResponse;
import com.zipdaproperty.domain.property.response.PropertyMapBoundsResponse;
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

    @Transactional(readOnly = true)
    public PropertyMapBoundsResponse findProperties(
            PropertyMapBoundsRequest request
    ) {
        PropertyMapBounds bounds =
                request.toBounds();

        long totalCount =
                propertyMapBoundsQueryRepository
                        .countPublicPropertiesInBounds(
                                bounds
                        );

        if (totalCount == 0L) {
            return new PropertyMapBoundsResponse(
                    List.of(),
                    0L,
                    false
            );
        }

        List<PropertyMapBoundsQueryRow> rows =
                propertyMapBoundsQueryRepository
                        .findPublicPropertiesInBounds(
                                bounds,
                                MAX_MAP_ITEMS
                        );

        List<PropertyMapBoundsItemResponse> items =
                rows.stream()
                        .map(this::toItemResponse)
                        .toList();

        return new PropertyMapBoundsResponse(
                items,
                totalCount,
                totalCount > MAX_MAP_ITEMS
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
