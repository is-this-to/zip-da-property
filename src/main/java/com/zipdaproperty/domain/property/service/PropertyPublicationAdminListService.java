package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.repository.PropertyPublicationAdminListQueryRepository;
import com.zipdaproperty.domain.property.request.PropertyPublicationAdminListRequest;
import com.zipdaproperty.domain.property.response.PropertyPublicationAdminListItemResponse;
import com.zipdaproperty.domain.property.response.PropertyPublicationAdminListResponse;
import com.zipdaproperty.global.context.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyPublicationAdminListService {
    private final PropertyPublicationAdminListQueryRepository repository;
    private final PropertyAdminReviewAccess access;

    @Transactional(readOnly = true)
    public PropertyPublicationAdminListResponse find(PropertyPublicationAdminListRequest request, ActorContext actor) {
        access.requireAdmin(actor);
        PropertyAdminReviewCursor cursor = PropertyAdminReviewCursor.decode(request.cursor());
        int size = request.size();
        List<PropertyPublicationAdminListItemResponse> rows = repository.find(
                request.status(), request.verificationStatus(), request.publisherType(),
                request.propertyType(), cursor.time(), cursor.id(), size + 1);
        boolean hasNext = rows.size() > size;
        List<PropertyPublicationAdminListItemResponse> items = rows.stream().limit(size).toList();
        String next = hasNext ? new PropertyAdminReviewCursor(
                items.getLast().createdAt(), items.getLast().propertyId()).encode() : null;
        return new PropertyPublicationAdminListResponse(items, next, hasNext);
    }
}
