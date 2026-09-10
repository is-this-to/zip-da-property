package com.zipdaproperty.domain.location.command;

import com.zipdaproperty.domain.location.model.VerifiedPropertyLocation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.Locale;

public record PublicLocationGenerationCommand(
        @NotNull(message = "매물 ID는 필수입니다.")
        @Positive(message = "매물 ID는 0보다 커야 합니다.")
        Long propertyId,

        @NotBlank(message = "정규화된 주소 해시는 필수입니다.")
        @Pattern(
                regexp = "^[0-9a-fA-F]{64}$",
                message = "정규화된 주소 해시는 SHA-256 형식이어야 합니다."
        )
        String normalizedAddressHash,

        @Valid
        @NotNull(message = "검증된 매물 위치는 필수입니다.")
        VerifiedPropertyLocation verifiedLocation
) {

    public PublicLocationGenerationCommand {
        if (normalizedAddressHash != null) {
            normalizedAddressHash = normalizedAddressHash
                    .trim()
                    .toLowerCase(Locale.ROOT);
        }
    }
}
