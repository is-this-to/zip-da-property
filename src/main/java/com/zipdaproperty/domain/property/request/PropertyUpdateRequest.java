package com.zipdaproperty.domain.property.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tools.jackson.databind.JsonNode;

import java.util.Map;

public record PropertyUpdateRequest(

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version,

        Map<String, JsonNode> changes,

        @Valid
        PropertyAddressRequest address

) {

    public PropertyUpdateRequest {
        changes = changes == null ? Map.of() : Map.copyOf(changes);
    }

    public PropertyUpdateRequest(
            Long version,
            Map<String, JsonNode> changes
    ) {
        this(version, changes, null);
    }
}
