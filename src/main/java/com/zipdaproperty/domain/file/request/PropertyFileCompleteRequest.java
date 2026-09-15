package com.zipdaproperty.domain.file.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PropertyFileCompleteRequest(

        @NotBlank(message = "체크섬은 필수입니다.")
        @Pattern(
                regexp = "^[0-9a-fA-F]{64}$",
                message = "체크섬은 64자리 SHA-256 hexadecimal 문자열이어야 합니다."
        )
        String checksum,

        @NotNull(message = "파일 크기는 필수입니다.")
        @Positive(message = "파일 크기는 0보다 커야 합니다.")
        Long size
) {
}
