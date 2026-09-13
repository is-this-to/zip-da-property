package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.model.PropertyListBounds;
import com.zipdaproperty.domain.property.model.PropertyMapSearchCondition;
import com.zipdaproperty.domain.property.model.PropertyPublicListCursor;
import com.zipdaproperty.domain.property.repository.PropertyPublicListQueryRepository;
import com.zipdaproperty.domain.property.repository.PropertyPublicListQueryRow;
import com.zipdaproperty.domain.property.request.PropertyPublicListRequest;
import com.zipdaproperty.domain.property.response.PropertyPublicListItemResponse;
import com.zipdaproperty.domain.property.response.PropertyPublicListResponse;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyPublicListService {

    private final PropertyPublicListQueryRepository queryRepository;
    private final PropertyPublicListCursorCodec cursorCodec;
    private final PropertyListSearchContextHasher contextHasher;

    @Transactional(readOnly = true)
    public PropertyPublicListResponse findProperties(PropertyPublicListRequest request) {
        PropertyListBounds bounds = request.toBounds();
        PropertyMapSearchCondition condition = request.toSearchCondition();
        String contextHash = contextHasher.hash(bounds, condition);
        PropertyPublicListCursor cursor = cursorCodec.decode(
                request.cursor(),
                condition.sort(),
                contextHash
        );

        int size = request.size();
        List<PropertyPublicListQueryRow> rows = queryRepository.findPublicProperties(
                bounds,
                condition,
                cursor,
                size + 1
        );
        boolean hasNext = rows.size() > size;
        List<PropertyPublicListQueryRow> visibleRows = rows.stream()
                .limit(size)
                .toList();

        String nextCursor = hasNext
                ? cursorCodec.encode(toCursor(
                        visibleRows.get(visibleRows.size() - 1),
                        condition,
                        contextHash
                ))
                : null;

        return new PropertyPublicListResponse(
                visibleRows.stream().map(this::toResponse).toList(),
                nextCursor,
                hasNext
        );
    }

    private PropertyPublicListCursor toCursor(
            PropertyPublicListQueryRow row,
            PropertyMapSearchCondition condition,
            String contextHash
    ) {
        return switch (condition.sort()) {
            case LATEST -> new PropertyPublicListCursor(
                    condition.sort(), contextHash, row.createdAt(), null, null, null, row.propertyId()
            );
            case PRICE_ASC, PRICE_DESC -> new PropertyPublicListCursor(
                    condition.sort(), contextHash, null,
                    row.representativePrice(), row.deposit(), null, row.propertyId()
            );
            case AREA_DESC -> new PropertyPublicListCursor(
                    condition.sort(), contextHash, null, null, null,
                    row.exclusiveArea(), row.propertyId()
            );
        };
    }

    private PropertyPublicListItemResponse toResponse(PropertyPublicListQueryRow row) {
        Point publicLocation = row.publicLocation();
        return new PropertyPublicListItemResponse(
                row.propertyId(),
                row.regionId(),
                row.title(),
                row.propertyType(),
                row.transactionType(),
                row.salePrice(),
                row.deposit(),
                row.monthlyRent(),
                row.maintenanceFee(),
                row.exclusiveArea(),
                row.roomCount(),
                row.publisherType(),
                row.publicAddress(),
                publicLocation.getY(),
                publicLocation.getX()
        );
    }
}
