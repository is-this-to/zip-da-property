package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.global.id.TsidString;

public record PropertyPublicDetailImageResponse(
        @TsidString Long fileId,
        String imageUrl,
        Integer sortOrder,
        Boolean representative
) {
}
