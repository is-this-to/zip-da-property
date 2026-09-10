package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.constant.ImageFileType;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.request.PropertyFileCompleteRequest;
import com.zipdaproperty.domain.file.storage.MinioObjectVerification;
import com.zipdaproperty.domain.file.storage.MinioObjectVerifier;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.business.InvalidFileTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyFileCompleteServiceTest {

    private static final long FILE_ID = 5_000_000_000_000_001L;
    private static final long FILE_SIZE = 1024L;
    private static final String CHECKSUM = "a".repeat(64);
    private static final ActorContext ACTOR_CONTEXT = ActorContext.member(
            101L,
            ActorRole.USER,
            "file-complete-test"
    );

    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);
    private final MinioObjectVerifier minioObjectVerifier =
            mock(MinioObjectVerifier.class);

    private PropertyFileCompleteService service;
    private PropertyFile propertyFile;

    @BeforeEach
    void setUp() {
        service = new PropertyFileCompleteService(
                propertyFileRepository,
                minioObjectVerifier
        );
        propertyFile = PropertyFile.create(
                FILE_ID,
                "upload-session",
                "photo.gif",
                FILE_SIZE,
                "opaque-test-value",
                Instant.now().plusSeconds(60),
                ACTOR_CONTEXT
        );
        when(propertyFileRepository.findByPropertyFileIdAndDeletedAtIsNull(
                FILE_ID
        )).thenReturn(Optional.of(propertyFile));
    }

    @Test
    void complete_gifContent_completesUpload() {
        when(minioObjectVerifier.verify("opaque-test-value"))
                .thenReturn(new MinioObjectVerification(
                        FILE_SIZE,
                        CHECKSUM,
                        ImageFileType.GIF
                ));

        service.complete(
                FILE_ID,
                new PropertyFileCompleteRequest(CHECKSUM, FILE_SIZE),
                ACTOR_CONTEXT
        );

        assertThat(propertyFile.getChecksum()).isEqualTo(CHECKSUM);
        verify(propertyFileRepository).save(propertyFile);
    }

    @Test
    void complete_gifExtensionWithNonGifContent_throwsInvalidFileTypeException() {
        when(minioObjectVerifier.verify("opaque-test-value"))
                .thenReturn(new MinioObjectVerification(
                        FILE_SIZE,
                        CHECKSUM,
                        ImageFileType.JPEG
                ));

        assertThatThrownBy(() -> service.complete(
                FILE_ID,
                new PropertyFileCompleteRequest(CHECKSUM, FILE_SIZE),
                ACTOR_CONTEXT
        )).isInstanceOf(InvalidFileTypeException.class);

        verify(propertyFileRepository, never()).save(propertyFile);
    }
}
