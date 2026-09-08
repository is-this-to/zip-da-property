package com.zipdaproperty.domain.property.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tools.jackson.databind.JsonNode;

import java.util.Map;

public record PropertyUpdateRequest(

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version,

        @NotEmpty(message = "changes에는 최소 한 개 이상의 수정 필드가 필요합니다.")
        Map<String, JsonNode> changes

) {
}