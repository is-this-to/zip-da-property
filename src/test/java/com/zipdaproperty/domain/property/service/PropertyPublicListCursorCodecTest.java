package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PropertyMapSort;
import com.zipdaproperty.domain.property.model.PropertyListCursorProperties;
import com.zipdaproperty.domain.property.model.PropertyPublicListCursor;
import com.zipdaproperty.global.error.custom.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyPublicListCursorCodecTest {

    private PropertyPublicListCursorCodec codec;

    @BeforeEach
    void setUp() {
        codec = new PropertyPublicListCursorCodec(
                new PropertyListCursorProperties(
                        "test-secret-with-enough-entropy",
                        "v1"
                )
        );
    }

    @Test
    void encodeAndDecodeLatestCursor() {
        PropertyPublicListCursor source = new PropertyPublicListCursor(
                PropertyMapSort.LATEST,
                "context",
                Instant.parse("2026-09-13T01:02:03.123456Z"),
                null,
                null,
                null,
                100L
        );

        PropertyPublicListCursor decoded = codec.decode(
                codec.encode(source),
                PropertyMapSort.LATEST,
                "context"
        );

        assertThat(decoded).isEqualTo(source);
    }

    @Test
    void encodeAndDecodePriceCursorWithNullDeposit() {
        PropertyPublicListCursor source = new PropertyPublicListCursor(
                PropertyMapSort.PRICE_ASC,
                "context",
                null,
                500_000_000L,
                null,
                null,
                101L
        );

        assertThat(codec.decode(
                codec.encode(source),
                PropertyMapSort.PRICE_ASC,
                "context"
        )).isEqualTo(source);
    }

    @Test
    void encodeAndDecodeAreaCursor() {
        PropertyPublicListCursor source = new PropertyPublicListCursor(
                PropertyMapSort.AREA_DESC,
                "context",
                null,
                null,
                null,
                new BigDecimal("84.50"),
                102L
        );

        PropertyPublicListCursor decoded = codec.decode(
                codec.encode(source),
                PropertyMapSort.AREA_DESC,
                "context"
        );

        assertThat(decoded.exclusiveArea()).isEqualByComparingTo("84.5");
    }

    @Test
    void rejectTamperedCursor() {
        PropertyPublicListCursor source = new PropertyPublicListCursor(
                PropertyMapSort.LATEST,
                "context",
                Instant.now(),
                null,
                null,
                null,
                100L
        );
        String token = codec.encode(source);
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> codec.decode(
                tampered,
                PropertyMapSort.LATEST,
                "context"
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectCursorWhenSearchContextChanges() {
        PropertyPublicListCursor source = new PropertyPublicListCursor(
                PropertyMapSort.LATEST,
                "old-context",
                Instant.now(),
                null,
                null,
                null,
                100L
        );

        assertThatThrownBy(() -> codec.decode(
                codec.encode(source),
                PropertyMapSort.LATEST,
                "new-context"
        )).isInstanceOf(BusinessException.class);
    }
}
