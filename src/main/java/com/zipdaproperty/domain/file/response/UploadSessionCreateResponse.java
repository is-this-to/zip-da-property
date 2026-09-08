package com.zipdaproperty.domain.file.response;

import java.time.Instant;
import java.util.List;

public record UploadSessionCreateResponse(
        String uploadSessionId,
        Instant expiresAt,
        List<UploadSessionFileResponse> files
) {
}
