package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.config.MinioImageProperties;
import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.constant.FileUploadPolicy;
import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.request.UploadFileRequest;
import com.zipdaproperty.domain.file.request.UploadSessionCreateRequest;
import com.zipdaproperty.domain.file.response.UploadSessionCreateResponse;
import com.zipdaproperty.domain.file.storage.MinioPresignedUploadUrlGenerator;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.FileTooLargeException;
import com.zipdaproperty.global.error.custom.business.InvalidFileTypeException;
import com.zipdaproperty.global.id.TsidGenerator;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadSessionServiceTest {

    private static final long FILE_ID = 5_000_000_000_000_001L;
    private static final long OWNER_MEMBER_ID = 101L;
    private static final List<String> ALLOWED_IMAGE_MIME_TYPES = List.of(
            "image/jpg",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );
    private static final ActorContext ACTOR_CONTEXT = ActorContext.member(
            OWNER_MEMBER_ID,
            ActorRole.USER,
            "file-upload-test"
    );

    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);
    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);
    private final MinioPresignedUploadUrlGenerator uploadUrlGenerator =
            mock(MinioPresignedUploadUrlGenerator.class);

    private UploadSessionService service;

    @BeforeEach
    void setUp() {
        service = new UploadSessionService(
                propertyFileRepository,
                tsidGenerator,
                uploadUrlGenerator,
                new MinioImageProperties(ALLOWED_IMAGE_MIME_TYPES)
        );
        ReflectionTestUtils.setField(
                service,
                "minioImagePath",
                "images"
        );
        when(tsidGenerator.generate()).thenReturn(FILE_ID);
        when(propertyFileRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(uploadUrlGenerator.generate(
                anyString(),
                any(Duration.class)
        )).thenReturn("https://example.test/upload");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "photo.jpg",
            "photo.jpeg",
            "photo.png",
            "photo.webp"
    })
    void create_supportedImage_createsUploadSession(String fileName) {
        UploadSessionCreateResponse response = service.create(
                request(fileName, 1024L),
                ACTOR_CONTEXT
        );

        assertThat(response.uploadSessionId()).isNotBlank();
        assertThat(response.expiresAt()).isNotNull();
        assertThat(response.files()).hasSize(1);
        assertThat(response.files().getFirst().fileId()).isEqualTo(FILE_ID);
        assertThat(response.files().getFirst().uploadUrl())
                .isEqualTo("https://example.test/upload");
        assertThat(response.files().getFirst().requiredHeaders()).isEmpty();
    }

    @Test
    void create_gif_createsUploadSession() {
        UploadSessionCreateResponse response = service.create(
                request("photo.GIF", 1024L),
                ACTOR_CONTEXT
        );

        assertThat(response.files()).hasSize(1);
        verify(propertyFileRepository).saveAll(anyList());
        verify(uploadUrlGenerator).generate(
                anyString(),
                any(Duration.class)
        );
    }

    @Test
    void create_unsupportedExtension_throwsInvalidFileTypeException() {
        assertThatThrownBy(() -> service.create(
                request("photo.bmp", 1024L),
                ACTOR_CONTEXT
        ))
                .isInstanceOf(InvalidFileTypeException.class)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.INVALID_FILE_TYPE)
                );

        verify(propertyFileRepository, never()).saveAll(anyList());
    }

    @Test
    void create_fileLargerThanTwentyMegabytes_throwsFileTooLargeException() {
        assertThatThrownBy(() -> service.create(
                request(
                        "photo.jpg",
                        FileUploadPolicy.MAX_FILE_SIZE_BYTES + 1
                ),
                ACTOR_CONTEXT
        ))
                .isInstanceOf(FileTooLargeException.class)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.FILE_TOO_LARGE)
                );

        verify(propertyFileRepository, never()).saveAll(anyList());
    }

    @Test
    void create_twentyMegabyteFile_createsUploadSession() {
        UploadSessionCreateResponse response = service.create(
                request(
                        "photo.jpg",
                        FileUploadPolicy.MAX_FILE_SIZE_BYTES
                ),
                ACTOR_CONTEXT
        );

        assertThat(response.files()).hasSize(1);
    }

    @Test
    void create_emptyFiles_throwsInvalidRequestException() {
        assertInvalidRequest(new UploadSessionCreateRequest(
                FilePurpose.PROPERTY_IMAGE,
                List.of()
        ));
    }

    @Test
    void create_moreThanThirtyFiles_throwsInvalidRequestException() {
        List<UploadFileRequest> files = new ArrayList<>();
        for (int index = 0; index < 31; index++) {
            files.add(new UploadFileRequest("photo-" + index + ".jpg", 1L));
        }

        assertInvalidRequest(new UploadSessionCreateRequest(
                FilePurpose.PROPERTY_IMAGE,
                files
        ));
    }

    @Test
    void create_validRequest_usesGeneratedFileIdAndActorMemberId() {
        service.create(
                request("photo.jpg", 1024L),
                ACTOR_CONTEXT
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PropertyFile>> filesCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(propertyFileRepository).saveAll(filesCaptor.capture());
        PropertyFile savedFile = filesCaptor.getValue().getFirst();
        assertThat(savedFile.getPropertyFileId()).isEqualTo(FILE_ID);
        assertThat(savedFile.getOwnerMemberId()).isEqualTo(OWNER_MEMBER_ID);
        assertThat(savedFile.getFilePurpose())
                .isEqualTo(FilePurpose.PROPERTY_IMAGE);
        assertThat(savedFile.getUploadStatus())
                .isEqualTo(UploadStatus.CREATED);
        assertThat(savedFile.getMimeType()).isNull();
        assertThat(savedFile.getLinkedAt()).isNull();
        assertThat(savedFile.getObjectDeletedAt()).isNull();
    }

    @Test
    void create_nullFilePurpose_throwsInvalidRequestException() {
        assertInvalidRequest(new UploadSessionCreateRequest(
                null,
                List.of(new UploadFileRequest("photo.jpg", 1024L))
        ));
    }

    @Test
    void create_multipleFiles_storesSameRequestedPurposeAndCreatedStatus() {
        when(tsidGenerator.generate()).thenReturn(FILE_ID, FILE_ID + 1);
        service.create(
                new UploadSessionCreateRequest(
                        FilePurpose.REPORT_EVIDENCE,
                        List.of(
                                new UploadFileRequest("first.jpg", 1024L),
                                new UploadFileRequest("second.png", 2048L)
                        )
                ),
                ACTOR_CONTEXT
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PropertyFile>> filesCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(propertyFileRepository).saveAll(filesCaptor.capture());
        assertThat(filesCaptor.getValue())
                .hasSize(2)
                .allSatisfy(propertyFile -> {
                    assertThat(propertyFile.getFilePurpose())
                            .isEqualTo(FilePurpose.REPORT_EVIDENCE);
                    assertThat(propertyFile.getUploadStatus())
                            .isEqualTo(UploadStatus.CREATED);
                });
    }

    @Test
    void create_extensionMissingFromConfiguration_throwsInvalidFileTypeException() {
        service = new UploadSessionService(
                propertyFileRepository,
                tsidGenerator,
                uploadUrlGenerator,
                new MinioImageProperties(List.of("image/png"))
        );
        ReflectionTestUtils.setField(
                service,
                "minioImagePath",
                "images"
        );

        assertThatThrownBy(() -> service.create(
                request("photo.jpg", 1024L),
                ACTOR_CONTEXT
        )).isInstanceOf(InvalidFileTypeException.class);

        verify(propertyFileRepository, never()).saveAll(anyList());
    }

    private UploadSessionCreateRequest request(
            String fileName,
            long fileSize
    ) {
        return new UploadSessionCreateRequest(
                FilePurpose.PROPERTY_IMAGE,
                List.of(new UploadFileRequest(fileName, fileSize))
        );
    }

    private void assertInvalidRequest(UploadSessionCreateRequest request) {
        assertThatThrownBy(() -> service.create(request, ACTOR_CONTEXT))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.INVALID_REQUEST)
                );
        verify(propertyFileRepository, never()).saveAll(anyList());
    }
}
