package com.zipdaproperty.domain.file.storage;

import com.zipdaproperty.domain.file.constant.ImageFileType;
import com.zipdaproperty.domain.file.constant.FileUploadPolicy;
import com.zipdaproperty.global.error.custom.business.FileTooLargeException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioObjectVerifierTest {

    private final MinioClient minioClient = mock(MinioClient.class);

    private MinioObjectVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new MinioObjectVerifier(minioClient);
        ReflectionTestUtils.setField(verifier, "bucket", "test-bucket");
    }

    @Test
    void verify_objectLargerThanTwentyMegabytes_doesNotGetObject()
            throws Exception {
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.size()).thenReturn(FileUploadPolicy.MAX_FILE_SIZE_BYTES + 1);
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenReturn(stat);

        assertThatThrownBy(() -> verifier.verify("opaque-test-value"))
                .isInstanceOf(FileTooLargeException.class);

        verify(minioClient, never()).getObject(any(GetObjectArgs.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GIF87a", "GIF89a"})
    void inspectStream_validGifSignature_detectsGif(String signature)
            throws Exception {
        byte[] content = signature.getBytes(StandardCharsets.US_ASCII);

        MinioObjectVerification verification = verifier.inspectStream(
                new ByteArrayInputStream(content),
                content.length
        );

        assertThat(verification.imageFileType()).isEqualTo(ImageFileType.GIF);
    }

    @Test
    void inspectStream_invalidGifSignature_detectsUnknown() throws Exception {
        byte[] content = "GIF88a".getBytes(StandardCharsets.US_ASCII);

        MinioObjectVerification verification = verifier.inspectStream(
                new ByteArrayInputStream(content),
                content.length
        );

        assertThat(verification.imageFileType())
                .isEqualTo(ImageFileType.UNKNOWN);
    }
}
