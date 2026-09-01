package com.zipdaproperty.domain.favorite.service;

import com.zipdaproperty.domain.favorite.entity.PropertyFavorite;
import com.zipdaproperty.domain.favorite.repository.PropertyFavoriteRepository;
import com.zipdaproperty.domain.favorite.repository.PropertyFavoriteTargetQueryRepository;
import com.zipdaproperty.domain.favorite.response.PropertyFavoriteUpdateResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PropertyFavoriteCommandService {

    private final PropertyFavoriteRepository propertyFavoriteRepository;
    private final PropertyFavoriteTargetQueryRepository propertyFavoriteTargetQueryRepository;

    @Transactional
    public PropertyFavoriteUpdateResponse updateFavoriteInTransaction(
            Long propertyId,
            boolean favorite,
            ActorContext actorContext
    ) {
        validateFavoriteActor(actorContext);
        validateFavoriteTarget(propertyId);

        Optional<PropertyFavorite> activeFavorite =
                propertyFavoriteRepository
                        .findByMemberIdAndPropertyIdAndDeletedAtIsNull(
                                actorContext.memberId(),
                                propertyId
                        );

        if (favorite && activeFavorite.isEmpty()) {
            propertyFavoriteRepository.saveAndFlush(
                    new PropertyFavorite(
                            actorContext.memberId(),
                            propertyId,
                            actorContext
                    )
            );
        }

        if (!favorite) {
            activeFavorite.ifPresent(
                    propertyFavorite ->
                            propertyFavorite.remove(
                                    actorContext,
                                    Instant.now()
                            )
            );
        }

        long favoriteCount =
                propertyFavoriteRepository
                        .countByPropertyIdAndDeletedAtIsNull(propertyId);

        return new PropertyFavoriteUpdateResponse(
                propertyId,
                favorite,
                favoriteCount
        );
    }

    private void validateFavoriteActor(ActorContext actorContext) {
        boolean allowedRole =
                actorContext.role() == ActorRole.USER
                        || actorContext.role() == ActorRole.AGENT;

        if (!actorContext.isMemberRequest() || !allowedRole) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "찜 기능을 사용할 수 없는 요청 주체입니다."
            );
        }
    }

    private void validateFavoriteTarget(Long propertyId) {
        boolean publiclyAvailable =
                propertyFavoriteTargetQueryRepository
                        .existsPubliclyAvailable(propertyId);

        if (!publiclyAvailable) {
            throw new BusinessException(
                    CustomResponseCode.FAVORITE_TARGET_UNAVAILABLE,
                    "찜 가능한 매물이 아닙니다."
            );
        }
    }
}
