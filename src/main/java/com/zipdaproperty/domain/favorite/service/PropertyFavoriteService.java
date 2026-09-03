package com.zipdaproperty.domain.favorite.service;

import com.zipdaproperty.domain.favorite.repository.PropertyFavoriteListQueryRow;
import com.zipdaproperty.domain.favorite.repository.PropertyFavoriteListQueryRepository;
import com.zipdaproperty.domain.favorite.response.PropertyFavoriteListResponse;
import com.zipdaproperty.domain.favorite.response.PropertyFavoriteUpdateResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyFavoriteService {

    private final PropertyFavoriteCommandService propertyFavoriteCommandService;
    private final PropertyFavoriteListQueryRepository propertyFavoriteListQueryRepository;

    public PropertyFavoriteUpdateResponse updateFavorite(
            Long propertyId,
            boolean favorite,
            ActorContext actorContext
    ) {
        try {
            return propertyFavoriteCommandService
                    .updateFavoriteInTransaction(
                            propertyId,
                            favorite,
                            actorContext
                    );
        } catch (DataIntegrityViolationException exception) {
            if (!favorite) {
                throw exception;
            }

            return propertyFavoriteCommandService
                    .updateFavoriteInTransaction(
                            propertyId,
                            true,
                            actorContext
                    );
        }
    }

    @Transactional(readOnly = true)
    public PropertyFavoriteListResponse getMyFavorites(
            ActorContext actorContext,
            int cursor,
            int size
    ) {
        validateFavoriteActor(actorContext);
        validatePagination(cursor, size);

        long offset = (long) cursor * size;
        List<PropertyFavoriteListQueryRow> rows =
                propertyFavoriteListQueryRepository.findMyFavorites(
                        actorContext.memberId(),
                        offset,
                        size + 1
                );

        boolean hasNext = rows.size() > size;
        List<PropertyFavoriteListResponse.Item> items = rows.stream()
                .limit(size)
                .map(this::toItem)
                .toList();
        Integer nextCursor = hasNext
                ? nextCursor(cursor)
                : null;

        return new PropertyFavoriteListResponse(
                items,
                nextCursor,
                hasNext
        );
    }

    private PropertyFavoriteListResponse.Item toItem(
            PropertyFavoriteListQueryRow row
    ) {
        return new PropertyFavoriteListResponse.Item(
                row.propertyId(),
                null,
                row.propertyType(),
                row.transactionType(),
                new PropertyFavoriteListResponse.Price(
                        row.salePrice(),
                        row.deposit(),
                        row.monthlyRent()
                ),
                row.exclusiveArea(),
                row.locationSummary(),
                row.floor(),
                row.publisherType(),
                null,
                row.favoriteCount(),
                true
        );
    }

    private void validateFavoriteActor(ActorContext actorContext) {
        boolean allowedRole =
                actorContext.role() == ActorRole.USER
                        || actorContext.role() == ActorRole.AGENT;

        if (!actorContext.isMemberRequest() || !allowedRole) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "찜 목록을 조회할 수 없는 요청 주체입니다."
            );
        }
    }

    private void validatePagination(int cursor, int size) {
        if (cursor < 0 || size < 1 || size > 50) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "cursor 또는 size가 허용 범위를 벗어났습니다."
            );
        }
    }

    private Integer nextCursor(int cursor) {
        if (cursor == Integer.MAX_VALUE) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "cursor가 허용 범위를 벗어났습니다."
            );
        }

        return cursor + 1;
    }
}
