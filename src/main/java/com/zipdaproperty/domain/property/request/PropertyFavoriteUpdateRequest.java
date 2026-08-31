package com.zipdaproperty.domain.property.request;

import jakarta.validation.constraints.NotNull;

public record PropertyFavoriteUpdateRequest(
        @NotNull(message = "찜 상태는 필수입니다.")
        Boolean favorite
) {
}