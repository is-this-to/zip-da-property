package com.zipdaproperty.domain.file.storage;

import com.zipdaproperty.global.error.custom.business.FileManagedException;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinioObjectDeleter {

    private final MinioClient minioClient;

    @Value("${minio.minio-bucket}")
    private String bucket;

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception exception) {
            throw new FileManagedException(
                    "스토리지 객체를 삭제할 수 없습니다."
            );
        }
    }
}
