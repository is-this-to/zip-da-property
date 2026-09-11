package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.global.id.TsidString;

public record PropertyEditImageResponse(

        @TsidString
        Long fileId,

        String imageUrl,

        Integer sortOrder,

        Boolean representative
) {
}
