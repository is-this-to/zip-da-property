package com.zipdaproperty.domain.property.request;

import com.zipdaproperty.domain.property.constant.PublicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PropertyPublicationStatusChangeRequest(

        @NotNull(message = "targetStatus는 필수입니다.")
        PublicationStatus targetStatus,

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version,

        @NotBlank(message = "reason은 필수입니다.")
        @Size(
                min = 1,
                max = 200,
                message = "reason은 1자 이상 200자 이하여야 합니다."
        )
        String reason

) {
}
