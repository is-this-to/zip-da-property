package com.zipdaproperty.domain.file.response;

import com.zipdaproperty.global.id.TsidString;

import java.util.Map;

public record UploadSessionFileResponse(

        @TsidString
        Long fileId,

        String uploadUrl,

        Map<String, String> requiredHeaders
) {
}
