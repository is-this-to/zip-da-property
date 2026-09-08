package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.exception.FileTooLargeException;
import com.zipdaproperty.domain.file.exception.InvalidFileTypeException;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.request.UploadFileRequest;
import com.zipdaproperty.domain.file.request.UploadSessionCreateRequest;
import com.zipdaproperty.domain.file.response.UploadSessionCreateResponse;
import com.zipdaproperty.domain.file.storage.MinioPresignedUploadUrlGenerator;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.id.TsidGenerator;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadSessionServiceTest {

    private static final long FILE_ID = 5_000_000_000_000_001L;
    private static final long OWNER_MEMBER_ID = 101L;
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
                uploadUrlGenerator
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
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Duration.class)
        )).thenReturn("https://minio.example/upload");
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
                .isEqualTo("https://minio.example/upload");
        assertThat(response.files().getFirst().requiredHeaders()).isEmpty();

        ArgumentCaptor<Duration> validityCaptor =
                ArgumentCaptor.forClass(Duration.class);
        verify(uploadUrlGenerator).generate(
                org.mockito.ArgumentMatchers.anyString(),
                validityCaptor.capture()
        );
        assertThat(validityCaptor.getValue())
                .isPositive()
                .isLessThanOrEqualTo(UploadSessionService.UPLOAD_SESSION_TTL);
    }

    @Test
    void create_moreThanThirtyFiles_rejects() {
        List<UploadFileRequest> files = new ArrayList<>();
        for (int index = 0; index < 31; index++) {
            files.add(new UploadFileRequest("photo-" + index + ".jpg", 1L));
        }

        assertInvalidRequest(new UploadSessionCreateRequest(files));
    }

    @Test
    void create_emptyFiles_rejects() {
        assertInvalidRequest(new UploadSessionCreateRequest(List.of()));
    }

    @Test
    void create_fileLargerThanTwentyMegabytes_rejects() {
        assertThatThrownBy(() -> service.create(
                request("photo.jpg", UploadSessionService.MAX_FILE_SIZE_BYTES + 1),
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
    void create_gif_rejects() {
        assertInvalidFileType("photo.gif");
    }

    @Test
    void create_unsupportedExtension_rejects() {
        assertInvalidFileType("photo.bmp");
    }

    @Test
    void create_doesNotExposeObjectKeyAndSerializesFileIdAsString() {
        UploadSessionCreateResponse response = service.create(
                request("photo.jpg", 1024L),
                ACTOR_CONTEXT
        );

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).doesNotContain("objectKey", "property-files/");
        assertThat(json).contains("\"fileId\":\"" + FILE_ID + "\"");
    }

    @Test
    void create_usesGeneratedTsidAndActorMemberAsOwner() {
        service.create(
                request("../safe-photo.jpg", 1024L),
                ACTOR_CONTEXT
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PropertyFile>> captor = ArgumentCaptor.forClass(List.class);
        verify(propertyFileRepository).saveAll(captor.capture());
        PropertyFile savedFile = captor.getValue().getFirst();

        assertThat(savedFile.getPropertyFileId()).isEqualTo(FILE_ID);
        assertThat(savedFile.getOwnerMemberId()).isEqualTo(OWNER_MEMBER_ID);
        assertThat(savedFile.getOriginalFileName()).isEqualTo("safe-photo.jpg");
        assertThat(savedFile.getObjectKey()).doesNotContain("safe-photo.jpg", "..");
        verify(tsidGenerator).generate();
    }

    private UploadSessionCreateRequest request(
            String name,
            long size
    ) {
        return new UploadSessionCreateRequest(
                List.of(new UploadFileRequest(name, size))
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

    private void assertInvalidFileType(String fileName) {
        assertThatThrownBy(() -> service.create(
                request(fileName, 1024L),
                ACTOR_CONTEXT
        ))
                .isInstanceOf(InvalidFileTypeException.class)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.INVALID_FILE_TYPE)
                );
        verify(propertyFileRepository, never()).saveAll(anyList());
    }
}
