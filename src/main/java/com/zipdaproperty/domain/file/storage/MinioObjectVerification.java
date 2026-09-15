package com.zipdaproperty.domain.file.storage;

import com.zipdaproperty.domain.file.constant.ImageFileType;

public record MinioObjectVerification(
        long size,
        String checksum,
        ImageFileType imageFileType
) {
}
