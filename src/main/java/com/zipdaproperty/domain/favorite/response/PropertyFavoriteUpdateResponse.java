package com.zipdaproperty.domain.favorite.response;

import com.zipdaproperty.global.id.TsidString;

public record PropertyFavoriteUpdateResponse(
        @TsidString
        Long propertyId,
        boolean favorite,
        long favoriteCount
) {
}
