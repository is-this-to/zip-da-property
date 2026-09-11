package com.zipdaproperty.domain.file.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioPresignedGetUrlGeneratorTest {

    private final MinioClient minioClient = mock(MinioClient.class);

    @Test
    void generate_usesIndependentFifteenMinuteGetPolicy() throws Exception {
        MinioPresignedGetUrlGenerator generator =
                new MinioPresignedGetUrlGenerator(minioClient);
        ReflectionTestUtils.setField(
                generator,
                "bucket",
                "test-bucket"
        );
        when(minioClient.getPresignedObjectUrl(
                org.mockito.ArgumentMatchers.any(
                        GetPresignedObjectUrlArgs.class
                )
        )).thenReturn("https://example.test/image");

        String imageUrl = generator.generate("opaque-image-reference");

        assertThat(imageUrl).isEqualTo("https://example.test/image");
        assertThat(MinioPresignedGetUrlGenerator.GET_URL_TTL)
                .isEqualTo(Duration.ofMinutes(15));
        ArgumentCaptor<GetPresignedObjectUrlArgs> captor =
                ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient).getPresignedObjectUrl(captor.capture());
        assertThat(captor.getValue().method()).isEqualTo(Method.GET);
        assertThat(captor.getValue().expiry()).isEqualTo(900);
    }
}
