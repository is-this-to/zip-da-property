package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public record PropertyMyListCursor(

        Instant updatedAt,

        Long propertyId

) {

    private static final String DELIMITER = ":";

    public PropertyMyListCursor {
        boolean onlyUpdatedAtExists =
                updatedAt != null
                        && propertyId == null;

        boolean onlyPropertyIdExists =
                updatedAt == null
                        && propertyId != null;

        if (onlyUpdatedAtExists
                || onlyPropertyIdExists) {
            throw new IllegalArgumentException(
                    "cursor 구성값은 모두 존재하거나 모두 없어야 합니다."
            );
        }

        if (propertyId != null
                && propertyId <= 0) {
            throw new IllegalArgumentException(
                    "cursor propertyId는 0보다 커야 합니다."
            );
        }
    }

    public static PropertyMyListCursor firstPage() {
        return new PropertyMyListCursor(
                null,
                null
        );
    }

    public static PropertyMyListCursor from(
            Instant updatedAt,
            Long propertyId
    ) {
        return new PropertyMyListCursor(
                Objects.requireNonNull(
                        updatedAt,
                        "cursor updatedAt은 필수입니다."
                ),
                Objects.requireNonNull(
                        propertyId,
                        "cursor propertyId는 필수입니다."
                )
        );
    }

    public static PropertyMyListCursor decode(
            String encodedCursor
    ) {
        if (encodedCursor == null) {
            return firstPage();
        }

        try {
            byte[] decodedBytes =
                    Base64.getUrlDecoder()
                            .decode(encodedCursor);

            String decodedValue =
                    new String(
                            decodedBytes,
                            StandardCharsets.UTF_8
                    );

            String[] cursorParts =
                    decodedValue.split(
                            DELIMITER,
                            -1
                    );

            if (cursorParts.length != 3) {
                throw new IllegalArgumentException(
                        "cursor 구성값 개수가 올바르지 않습니다."
                );
            }

            long epochSecond =
                    Long.parseLong(cursorParts[0]);

            int nano =
                    Integer.parseInt(cursorParts[1]);

            long propertyId =
                    Long.parseLong(cursorParts[2]);

            Instant updatedAt =
                    Instant.ofEpochSecond(
                            epochSecond,
                            nano
                    );

            return new PropertyMyListCursor(
                    updatedAt,
                    propertyId
            );
        } catch (
                IllegalArgumentException
                | DateTimeException exception
        ) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "cursor 형식이 올바르지 않습니다."
            );
        }
    }

    public String encode() {
        if (updatedAt == null
                || propertyId == null) {
            return null;
        }

        String rawCursor =
                updatedAt.getEpochSecond()
                        + DELIMITER
                        + updatedAt.getNano()
                        + DELIMITER
                        + propertyId;

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        rawCursor.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
    }
}