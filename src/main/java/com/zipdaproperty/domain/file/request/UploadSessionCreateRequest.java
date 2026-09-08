package com.zipdaproperty.domain.file.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UploadSessionCreateRequest(

        @NotEmpty(message = "파일 목록은 필수입니다.")
        @Size(
                max = 30,
                message = "파일은 한 번에 최대 30개까지 요청할 수 있습니다."
        )
        List<@NotNull(message = "파일 정보는 필수입니다.") @Valid UploadFileRequest> files
) {
}
