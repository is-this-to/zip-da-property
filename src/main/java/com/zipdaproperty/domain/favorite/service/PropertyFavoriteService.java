package com.zipdaproperty.domain.favorite.service;

import com.zipdaproperty.domain.favorite.response.PropertyFavoriteUpdateResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.domain.favorite.repository.PropertyFavoriteListQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PropertyFavoriteService {

    private final PropertyFavoriteCommandService propertyFavoriteCommandService;

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
}
