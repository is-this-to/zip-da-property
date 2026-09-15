package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.storage.MinioObjectDeleter;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyFileObjectDeletionServiceTest {

    private static final Long FILE_ID = 101L;
    private static final String OBJECT_KEY = "property-files/test/101.jpg";
    private static final ActorContext ACTOR_CONTEXT = ActorContext.member(
            1001L,
            ActorRole.USER,
            "file-object-deletion-test"
    );

    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);
    private final MinioObjectDeleter minioObjectDeleter =
            mock(MinioObjectDeleter.class);
    private final PropertyFileObjectDeletionMarker deletionMarker =
            new PropertyFileObjectDeletionMarker(propertyFileRepository);
    private final PropertyFileObjectDeletionService service =
            new PropertyFileObjectDeletionService(
                    propertyFileRepository,
                    minioObjectDeleter,
                    deletionMarker
            );

    @Test
    void deleteObject_success_recordsObjectDeletedAt() {
        PropertyFile propertyFile = deletedFile();
        when(propertyFileRepository.findById(FILE_ID))
                .thenReturn(Optional.of(propertyFile));

        service.deleteObject(FILE_ID);

        verify(minioObjectDeleter).delete(OBJECT_KEY);
        assertThat(propertyFile.getObjectDeletedAt()).isNotNull();
    }

    @Test
    void deleteObject_minioFailure_leavesObjectDeletedAtNullForRetry() {
        PropertyFile propertyFile = deletedFile();
        when(propertyFileRepository.findById(FILE_ID))
                .thenReturn(Optional.of(propertyFile));
        doThrow(new RuntimeException("minio unavailable"))
                .when(minioObjectDeleter)
                .delete(OBJECT_KEY);

        assertThatThrownBy(() -> service.deleteObject(FILE_ID))
                .isInstanceOf(RuntimeException.class);

        assertThat(propertyFile.getObjectDeletedAt()).isNull();
    }

    private PropertyFile deletedFile() {
        PropertyFile propertyFile = PropertyFile.create(
                FILE_ID,
                "upload-session",
                FilePurpose.PROPERTY_IMAGE,
                "photo.jpg",
                1024L,
                OBJECT_KEY,
                Instant.now().plusSeconds(900),
                ACTOR_CONTEXT
        );
        propertyFile.softDelete(
                ACTOR_CONTEXT,
                Instant.now(),
                "test deletion"
        );
        return propertyFile;
    }
}
