package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.repository.PropertyMyListQueryRepository;
import com.zipdaproperty.domain.property.repository.PropertyMyListQueryRow;
import com.zipdaproperty.domain.property.request.PropertyMyListRequest;
import com.zipdaproperty.domain.property.response.PropertyMyListItemResponse;
import com.zipdaproperty.domain.property.response.PropertyMyListResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyMyListService {

    private final PropertyMyListQueryRepository
            propertyMyListQueryRepository;

    @Transactional(readOnly = true)
    public PropertyMyListResponse findMyProperties(
            PropertyMyListRequest request,
            ActorContext actorContext
    ) {
        validateActor(actorContext);

        PropertyMyListCursor cursor =
                PropertyMyListCursor.decode(
                        request.cursor()
                );

        int size = request.size();

        List<PropertyMyListQueryRow> rows =
                propertyMyListQueryRepository
                        .findMyProperties(
                                actorContext.memberId(),
                                cursor.updatedAt(),
                                cursor.propertyId(),
                                size + 1
                        );

        boolean hasNext =
                rows.size() > size;

        List<PropertyMyListQueryRow> visibleRows =
                rows.stream()
                        .limit(size)
                        .toList();

        List<PropertyMyListItemResponse> items =
                visibleRows.stream()
                        .map(this::toItemResponse)
                        .toList();

        String nextCursor =
                createNextCursor(
                        visibleRows,
                        hasNext
                );

        return new PropertyMyListResponse(
                items,
                nextCursor,
                hasNext
        );
    }

    private void validateActor(
            ActorContext actorContext
    ) {
        if (actorContext == null
                || !actorContext.isMemberRequest()) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "회원 요청만 내 매물 목록을 조회할 수 있습니다."
            );
        }

        ActorRole actorRole =
                actorContext.role();

        boolean allowedRole =
                actorRole == ActorRole.USER
                        || actorRole == ActorRole.AGENT;

        if (!allowedRole) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "USER 또는 AGENT만 내 매물 목록을 조회할 수 있습니다."
            );
        }
    }

    private String createNextCursor(
            List<PropertyMyListQueryRow> visibleRows,
            boolean hasNext
    ) {
        if (!hasNext) {
            return null;
        }

        PropertyMyListQueryRow lastRow =
                visibleRows.get(
                        visibleRows.size() - 1
                );

        return PropertyMyListCursor
                .from(
                        lastRow.updatedAt(),
                        lastRow.propertyId()
                )
                .encode();
    }

    private PropertyMyListItemResponse toItemResponse(
            PropertyMyListQueryRow row
    ) {
        return new PropertyMyListItemResponse(
                row.propertyId(),
                row.version(),
                row.regionId(),
                row.title(),
                row.propertyType(),
                row.transactionType(),
                row.salePrice(),
                row.deposit(),
                row.monthlyRent(),
                row.publisherType(),
                row.publicationStatus(),
                row.transactionStatus(),
                row.verificationStatus(),
                row.updatedAt()
        );
    }
}