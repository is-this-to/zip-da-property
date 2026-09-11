package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.request.PropertyCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PropertyCreateRequestDeserializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deserialize_withAddressObjectCreatesRequest() {
        String json = """
                {
                  "regionId": "53390",
                  "publisherType": "DIRECT_OWNER",
                  "propertyType": "APARTMENT",
                  "transactionType": "SALE",
                  "salePrice": 510000000,
                  "exclusiveArea": 59.99,
                  "title": "Address integration test",
                  "description": "Property and address transaction test",
                  "address": {
                    "roadAddress": "Test road 100",
                    "jibunAddress": "Test jibun 100",
                    "legalDongCode": "9991010100",
                    "longitude": 128.625,
                    "latitude": 35.855
                  }
                }
                """;

        PropertyCreateRequest request = objectMapper.readValue(
                json,
                PropertyCreateRequest.class
        );

        assertThat(request.address()).isNotNull();
        assertThat(request.address().legalDongCode())
                .isEqualTo("9991010100");
        assertThat(request.toCommand().address().longitude())
                .isEqualByComparingTo("128.625");
    }
}
