package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.global.error.custom.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyAddressNormalizerTest {

    private final PropertyAddressNormalizer normalizer =
            new PropertyAddressNormalizer();

    @Test
    void createNormalizedAddressHash_prefersRoadAddressAndNormalizesWhitespace() {
        String firstHash = normalizer.createNormalizedAddressHash(
                "  대구 수성구   달구벌대로 2450  ",
                "대구광역시 수성구 범어동 123"
        );

        String secondHash = normalizer.createNormalizedAddressHash(
                "대구 수성구 달구벌대로 2450",
                "서로 다른 지번 주소"
        );

        assertThat(firstHash)
                .matches("^[0-9a-f]{64}$")
                .isEqualTo(secondHash);
    }

    @Test
    void createNormalizedAddressHash_withoutRoadAddressUsesJibunAddress() {
        String hash = normalizer.createNormalizedAddressHash(
                null,
                "  대구광역시   수성구 범어동 123  "
        );

        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void createNormalizedAddressHash_withoutAnyAddressThrowsInvalidRequest() {
        assertThatThrownBy(
                () -> normalizer.createNormalizedAddressHash(
                        "   ",
                        null
                )
        ).isInstanceOf(BusinessException.class);
    }
}
