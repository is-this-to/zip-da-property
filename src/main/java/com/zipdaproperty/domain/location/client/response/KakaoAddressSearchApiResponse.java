package com.zipdaproperty.domain.location.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoAddressSearchApiResponse(
        Meta meta,
        List<Document> documents
) {

    public KakaoAddressSearchApiResponse {
        documents = documents == null
                ? List.of()
                : List.copyOf(documents);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
            @JsonProperty("total_count")
            int totalCount,

            @JsonProperty("is_end")
            boolean end
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            @JsonProperty("address_name")
            String addressName,

            String x,
            String y,
            Address address,

            @JsonProperty("road_address")
            RoadAddress roadAddress
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
            @JsonProperty("address_name")
            String addressName,

            @JsonProperty("b_code")
            String legalDongCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoadAddress(
            @JsonProperty("address_name")
            String addressName,

            @JsonProperty("building_name")
            String buildingName
    ) {
    }
}
