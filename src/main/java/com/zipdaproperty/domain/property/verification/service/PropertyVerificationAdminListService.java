package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.property.service.PropertyAdminReviewAccess;
import com.zipdaproperty.domain.property.service.PropertyAdminReviewCursor;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationAdminListQueryRepository;
import com.zipdaproperty.domain.property.verification.request.PropertyVerificationAdminListRequest;
import com.zipdaproperty.domain.property.verification.response.PropertyVerificationAdminListItemResponse;
import com.zipdaproperty.domain.property.verification.response.PropertyVerificationAdminListResponse;
import com.zipdaproperty.global.context.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyVerificationAdminListService {
    private final PropertyVerificationAdminListQueryRepository repository;
    private final PropertyAdminReviewAccess access;

    @Transactional(readOnly = true)
    public PropertyVerificationAdminListResponse find(PropertyVerificationAdminListRequest request, ActorContext actor) {
        access.requireAdmin(actor);
        PropertyAdminReviewCursor cursor = PropertyAdminReviewCursor.decode(request.cursor());
        int size = request.size();
        List<PropertyVerificationAdminListItemResponse> rows = repository.find(
                request.status(), request.verificationType(), cursor.time(), cursor.id(), size + 1);
        boolean hasNext = rows.size() > size;
        List<PropertyVerificationAdminListItemResponse> items = rows.stream().limit(size).toList();
        String next = hasNext ? new PropertyAdminReviewCursor(
                items.getLast().submittedAt(), items.getLast().verificationId()).encode() : null;
        return new PropertyVerificationAdminListResponse(items, next, hasNext);
    }
}
