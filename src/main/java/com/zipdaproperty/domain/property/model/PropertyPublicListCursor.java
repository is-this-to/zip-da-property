package com.zipdaproperty.domain.property.model;

import com.zipdaproperty.domain.property.constant.PropertyMapSort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record PropertyPublicListCursor(
        PropertyMapSort sort,
        String contextHash,
        Instant createdAt,
        Long representativePrice,
        Long deposit,
        BigDecimal exclusiveArea,
        Long propertyId
) {

    public PropertyPublicListCursor {
        Objects.requireNonNull(sort, "cursor sort는 필수입니다.");
        Objects.requireNonNull(contextHash, "cursor contextHash는 필수입니다.");
        Objects.requireNonNull(propertyId, "cursor propertyId는 필수입니다.");

        if (contextHash.isBlank() || propertyId <= 0L) {
            throw new IllegalArgumentException("cursor 구성값이 올바르지 않습니다.");
        }

        switch (sort) {
            case LATEST -> Objects.requireNonNull(createdAt, "LATEST cursor createdAt은 필수입니다.");
            case PRICE_ASC, PRICE_DESC -> Objects.requireNonNull(
                    representativePrice,
                    "가격 cursor 대표 가격은 필수입니다."
            );
            case AREA_DESC -> Objects.requireNonNull(
                    exclusiveArea,
                    "AREA_DESC cursor 전용면적은 필수입니다."
            );
        }
    }
}
