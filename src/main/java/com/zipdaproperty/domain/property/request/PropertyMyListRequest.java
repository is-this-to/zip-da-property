package com.zipdaproperty.domain.property.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PropertyMyListRequest(

        @Size(
                min = 1,
                max = 500,
                message = "cursor는 1자 이상 500자 이하여야 합니다."
        )
        String cursor,

        @Min(
                value = 1,
                message = "size는 1 이상이어야 합니다."
        )
        @Max(
                value = 50,
                message = "size는 50 이하여야 합니다."
        )
        Integer size

) {

    private static final int DEFAULT_SIZE = 20;

    public PropertyMyListRequest {
        if (size == null) {
            size = DEFAULT_SIZE;
        }
    }
}