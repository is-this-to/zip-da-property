package com.zipdaproperty.domain.file.storage;

import com.zipdaproperty.global.error.custom.business.FileManagedException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class MinioPresignedGetUrlGenerator {

    static final Duration GET_URL_TTL = Duration.ofMinutes(15);

    private final MinioClient minioClient;

    @Value("${minio.minio-bucket}")
    private String bucket;

    public String generate(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(Math.toIntExact(GET_URL_TTL.toSeconds()))
                            .build()
            );
        } catch (Exception exception) {
            throw new FileManagedException(
                    "이미지 조회 URL 생성에 실패했습니다."
            );
        }
    }
}
