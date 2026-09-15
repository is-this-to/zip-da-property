package com.zipdaproperty.domain.property.idempotency.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyIdempotencyServiceTest {

    @Test
    void createPropertyEndpoint_matchesControllerRoute() {
        String endpoint = (String) ReflectionTestUtils.getField(
                PropertyIdempotencyService.class,
                "CREATE_PROPERTY_ENDPOINT"
        );

        assertThat(endpoint)
                .isEqualTo("POST:/api/property/properties");
    }
}
