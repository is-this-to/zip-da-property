package com.zipdaproperty.domain.property.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PropertyRestoreRequest(

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version,

        @NotBlank(message = "restoreReason은 필수입니다.")
        @Size(
                min = 1,
                max = 500,
                message = "restoreReason은 1자 이상 500자 이하여야 합니다."
        )
        String restoreReason

) {
}