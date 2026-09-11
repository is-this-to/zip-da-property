package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.config.MinioImageProperties;
import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.constant.ImageFileType;
import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.request.PropertyFileCompleteRequest;
import com.zipdaproperty.domain.file.storage.MinioObjectVerification;
import com.zipdaproperty.domain.file.storage.MinioObjectVerifier;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.InvalidFileTypeException;
import com.zipdaproperty.global.error.custom.business.FileOwnershipRequiredException;
import com.zipdaproperty.global.error.custom.business.NotFoundResourceException;
import com.zipdaproperty.global.error.custom.business.UnauthenticatedException;
import com.zipdaproperty.global.error.custom.business.UploadSessionExpiredException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyFileCompleteServiceTest {

    private static final long FILE_ID = 5_000_000_000_000_001L;
    private static final long FILE_SIZE = 1024L;
    private static final String CHECKSUM = "a".repeat(64);
    private static final List<String> ALLOWED_IMAGE_MIME_TYPES = List.of(
            "image/jpg",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );
    private static final ActorContext ACTOR_CONTEXT = ActorContext.member(
            101L,
            ActorRole.USER,
            "file-complete-test"
    );
    private static final ActorContext RETRY_ACTOR_CONTEXT = ActorContext.member(
            101L,
            ActorRole.AGENT,
            "file-complete-retry-test"
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
                minioObjectVerifier,
                new MinioImageProperties(ALLOWED_IMAGE_MIME_TYPES)
        );
        propertyFile = spy(PropertyFile.create(
                FILE_ID,
                "upload-session",
                FilePurpose.PROPERTY_IMAGE,
                "photo.gif",
                FILE_SIZE,
                "opaque-test-value",
                Instant.now().plusSeconds(60),
                ACTOR_CONTEXT
        ));
        when(propertyFileRepository.findByPropertyFileIdAndDeletedAtIsNull(
                FILE_ID
        )).thenReturn(Optional.of(propertyFile));
    }

    @ParameterizedTest
    @CsvSource({
            "jpg, JPEG, image/jpg",
            "jpeg, JPEG, image/jpeg",
            "png, PNG, image/png",
            "gif, GIF, image/gif",
            "webp, WEBP, image/webp"
    })
    void complete_supportedImage_storesVerifiedMimeType(
            String extension,
            ImageFileType imageFileType,
            String expectedMimeType
    ) {
        ReflectionTestUtils.setField(
                propertyFile,
                "originalFileName",
                "photo." + extension
        );
        when(minioObjectVerifier.verify("opaque-test-value"))
                .thenReturn(new MinioObjectVerification(
                        FILE_SIZE,
                        CHECKSUM,
                        imageFileType
                ));

        service.complete(
                FILE_ID,
                new PropertyFileCompleteRequest(CHECKSUM, FILE_SIZE),
                ACTOR_CONTEXT
        );

        assertThat(propertyFile.getChecksum()).isEqualTo(CHECKSUM);
        assertThat(propertyFile.getMimeType()).isEqualTo(expectedMimeType);
        assertThat(propertyFile.getUploadStatus())
                .isEqualTo(UploadStatus.VERIFIED);
        verify(propertyFile).complete(
                CHECKSUM,
                expectedMimeType,
                ACTOR_CONTEXT
        );
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

        assertThat(propertyFile.getUploadStatus())
                .isEqualTo(UploadStatus.CREATED);
        assertThat(propertyFile.getMimeType()).isNull();
        verify(propertyFile, never()).complete(
                anyString(),
                anyString(),
                any(ActorContext.class)
        );
        verify(propertyFileRepository, never()).save(any(PropertyFile.class));
    }

    @ParameterizedTest
    @EnumSource(
            value = UploadStatus.class,
            names = {"VERIFIED", "LINKED"}
    )
    void complete_completedFileWithSameRequest_returnsSuccessWithoutSideEffects(
            UploadStatus uploadStatus
    ) {
        prepareCompletedFile(uploadStatus, Instant.now().plusSeconds(60));

        assertThat(service.complete(
                FILE_ID,
                new PropertyFileCompleteRequest(CHECKSUM, FILE_SIZE),
                RETRY_ACTOR_CONTEXT
        )).isNotNull();

        verify(minioObjectVerifier, never()).verify(anyString());
        verify(propertyFileRepository, never()).save(any(PropertyFile.class));
        verifyCompleteWasNotCalled();
        assertThat(propertyFile.getUpdatedByRole()).isEqualTo(ActorRole.USER);
    }

    @Test
    void complete_expiredCompletedFileWithSameRequest_returnsSuccess() {
        prepareCompletedFile(
                UploadStatus.VERIFIED,
                Instant.now().minusSeconds(60)
        );

        assertThat(service.complete(
                FILE_ID,
                new PropertyFileCompleteRequest(CHECKSUM, FILE_SIZE),
                ACTOR_CONTEXT
        )).isNotNull();

        verify(minioObjectVerifier, never()).verify(anyString());
        verify(propertyFileRepository, never()).save(any(PropertyFile.class));
        verifyCompleteWasNotCalled();
    }

    @Test
    void complete_completedFileWithDifferentChecksum_throwsInvalidRequest() {
        prepareCompletedFile(
                UploadStatus.VERIFIED,
                Instant.now().plusSeconds(60)
        );

        assertInvalidRequest(new PropertyFileCompleteRequest(
                "b".repeat(64),
                FILE_SIZE
        ));

        verify(minioObjectVerifier, never()).verify(anyString());
        verify(propertyFileRepository, never()).save(any(PropertyFile.class));
        verifyCompleteWasNotCalled();
        assertThat(propertyFile.getUploadStatus())
                .isEqualTo(UploadStatus.VERIFIED);
    }

    @Test
    void complete_completedFileWithDifferentSize_throwsInvalidRequest() {
        prepareCompletedFile(
                UploadStatus.VERIFIED,
                Instant.now().plusSeconds(60)
        );

        assertInvalidRequest(new PropertyFileCompleteRequest(
                CHECKSUM,
                FILE_SIZE + 1
        ));

        verify(minioObjectVerifier, never()).verify(anyString());
        verify(propertyFileRepository, never()).save(any(PropertyFile.class));
        verifyCompleteWasNotCalled();
        assertThat(propertyFile.getUploadStatus())
                .isEqualTo(UploadStatus.VERIFIED);
    }

    @Test
    void complete_expiredCreatedFile_throwsUploadSessionExpiredException() {
        ReflectionTestUtils.setField(
                propertyFile,
                "expiresAt",
                Instant.now().minusSeconds(60)
        );

        assertThatThrownBy(() -> service.complete(
                FILE_ID,
                new PropertyFileCompleteRequest(CHECKSUM, FILE_SIZE),
                ACTOR_CONTEXT
        )).isInstanceOf(UploadSessionExpiredException.class);

        verify(minioObjectVerifier, never()).verify(anyString());
        verify(propertyFileRepository, never()).save(any(PropertyFile.class));
        verifyCompleteWasNotCalled();
        assertThat(propertyFile.getUploadStatus())
                .isEqualTo(UploadStatus.CREATED);
    }

    @Test
    void complete_nullActorContext_throwsUnauthenticatedWithoutQuery() {
        assertThatThrownBy(() -> service.complete(
                FILE_ID,
                new PropertyFileCompleteRequest(CHECKSUM, FILE_SIZE),
                null
        )).isInstanceOf(UnauthenticatedException.class);

        verifyNoInteractions(propertyFileRepository, minioObjectVerifier);
    }

    @Test
    void complete_otherOwner_throwsOwnershipRequired() {
        ActorContext other = ActorContext.member(
                999L, ActorRole.USER, "other-file-owner-test"
        );

        assertThatThrownBy(() -> service.complete(
                FILE_ID,
                new PropertyFileCompleteRequest(CHECKSUM, FILE_SIZE),
                other
        )).isInstanceOf(FileOwnershipRequiredException.class);

        verifyNoInteractions(minioObjectVerifier);
        verify(propertyFileRepository, never()).save(any(PropertyFile.class));
    }

    @Test
    void complete_missingOrDeletedFile_throwsNotFound() {
        when(propertyFileRepository.findByPropertyFileIdAndDeletedAtIsNull(FILE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(
                FILE_ID,
                new PropertyFileCompleteRequest(CHECKSUM, FILE_SIZE),
                ACTOR_CONTEXT
        )).isInstanceOf(NotFoundResourceException.class);

        verifyNoInteractions(minioObjectVerifier);
    }

    @Test
    void complete_actualSizeMismatch_throwsInvalidRequest() {
        when(minioObjectVerifier.verify("opaque-test-value"))
                .thenReturn(new MinioObjectVerification(
                        FILE_SIZE + 1, CHECKSUM, ImageFileType.GIF
                ));

        assertInvalidRequest(new PropertyFileCompleteRequest(CHECKSUM, FILE_SIZE));
        verify(propertyFileRepository, never()).save(any(PropertyFile.class));
    }

    @Test
    void complete_actualChecksumMismatch_throwsInvalidRequest() {
        when(minioObjectVerifier.verify("opaque-test-value"))
                .thenReturn(new MinioObjectVerification(
                        FILE_SIZE, "b".repeat(64), ImageFileType.GIF
                ));

        assertInvalidRequest(new PropertyFileCompleteRequest(CHECKSUM, FILE_SIZE));
        verify(propertyFileRepository, never()).save(any(PropertyFile.class));
    }

    @Test
    void complete_uppercaseChecksum_normalizesAndCompletes() {
        String uppercaseChecksum = "A".repeat(64);
        when(minioObjectVerifier.verify("opaque-test-value"))
                .thenReturn(new MinioObjectVerification(
                        FILE_SIZE, CHECKSUM, ImageFileType.GIF
                ));

        service.complete(
                FILE_ID,
                new PropertyFileCompleteRequest(uppercaseChecksum, FILE_SIZE),
                ACTOR_CONTEXT
        );

        verify(propertyFile).complete(CHECKSUM, "image/gif", ACTOR_CONTEXT);
    }

    private void prepareCompletedFile(
            UploadStatus uploadStatus,
            Instant expiresAt
    ) {
        ReflectionTestUtils.setField(propertyFile, "checksum", CHECKSUM);
        ReflectionTestUtils.setField(propertyFile, "uploadStatus", uploadStatus);
        ReflectionTestUtils.setField(propertyFile, "expiresAt", expiresAt);
    }

    private void verifyCompleteWasNotCalled() {
        verify(propertyFile, never()).complete(
                anyString(),
                anyString(),
                any(ActorContext.class)
        );
    }

    private void assertInvalidRequest(PropertyFileCompleteRequest request) {
        assertThatThrownBy(() -> service.complete(
                FILE_ID,
                request,
                ACTOR_CONTEXT
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.INVALID_REQUEST)
        );
    }
}
