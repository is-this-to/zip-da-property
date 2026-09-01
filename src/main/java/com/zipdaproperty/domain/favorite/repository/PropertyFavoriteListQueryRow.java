package com.zipdaproperty.domain.favorite.repository;

import java.time.Instant;

public record PropertyFavoriteListQueryRow(
        Long propertyId,
        Instant favoritedAt
) {
}
