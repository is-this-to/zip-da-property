package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyMyListCursorTest {

    private static final Long PROPERTY_ID =
            884685586571263701L;

    private static final Instant UPDATED_AT =
            Instant.parse(
                    "2026-09-09T06:41:04.828Z"
            );

    @Test
    void encodeAndDecode_validCursor_restoresOriginalValues() {
        PropertyMyListCursor originalCursor =
                PropertyMyListCursor.from(
                        UPDATED_AT,
                        PROPERTY_ID
                );

        String encodedCursor =
                originalCursor.encode();

        PropertyMyListCursor decodedCursor =
                PropertyMyListCursor.decode(
                        encodedCursor
                );

        assertThat(encodedCursor)
                .isNotBlank();

        assertThat(decodedCursor.updatedAt())
                .isEqualTo(UPDATED_AT);

        assertThat(decodedCursor.propertyId())
                .isEqualTo(PROPERTY_ID);
    }

    @Test
    void decode_nullCursor_returnsFirstPageCursor() {
        PropertyMyListCursor cursor =
                PropertyMyListCursor.decode(null);

        assertThat(cursor.updatedAt())
                .isNull();

        assertThat(cursor.propertyId())
                .isNull();

        assertThat(cursor.encode())
                .isNull();
    }

    @Test
    void decode_invalidCursor_throwsInvalidRequest() {
        assertThatThrownBy(
                () -> PropertyMyListCursor.decode(
                        "not-a-valid-cursor"
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode.INVALID_REQUEST
                        )
        );
    }

    @Test
    void constructor_partialValues_throwsIllegalArgumentException() {
        assertThatThrownBy(
                () -> new PropertyMyListCursor(
                        UPDATED_AT,
                        null
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }
}