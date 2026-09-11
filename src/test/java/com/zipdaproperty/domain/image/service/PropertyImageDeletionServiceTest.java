package com.zipdaproperty.domain.image.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.service.PropertyFileObjectDeletionPublisher;
import com.zipdaproperty.domain.image.entity.PropertyImage;
import com.zipdaproperty.domain.image.repository.PropertyImageRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyImageDeletionServiceTest {

    private static final Long PROPERTY_ID = 100L;
    private static final Long FILE_ID = 101L;
    private static final ActorContext ACTOR_CONTEXT = ActorContext.member(
            1001L,
            ActorRole.USER,
            "property-image-deletion-test"
    );

    private final PropertyImageRepository propertyImageRepository =
            mock(PropertyImageRepository.class);
    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);
    private final PropertyFileObjectDeletionPublisher deletionPublisher =
            mock(PropertyFileObjectDeletionPublisher.class);
    private final PropertyImageDeletionService service =
            new PropertyImageDeletionService(
                    propertyImageRepository,
                    propertyFileRepository,
                    deletionPublisher
            );

    @Test
    void deleteAllForProperty_softDeletesImageAndFileThenSchedulesObjectDeletion() {
        Instant deletedAt = Instant.parse("2026-09-11T03:00:00Z");
        PropertyImage image = PropertyImage.create(
                PROPERTY_ID,
                FILE_ID,
                0,
                true,
                null,
                ACTOR_CONTEXT
        );
        PropertyFile file = PropertyFile.create(
                FILE_ID,
                "upload-session",
                FilePurpose.PROPERTY_IMAGE,
                "photo.jpg",
                1024L,
                "property-files/test/101.jpg",
                Instant.now().plusSeconds(900),
                ACTOR_CONTEXT
        );
        when(propertyImageRepository
                .findAllByPropertyIdAndDeletedAtIsNullOrderBySortOrderAsc(
                        PROPERTY_ID
                )).thenReturn(List.of(image));
        when(propertyFileRepository
                .findAllByPropertyFileIdInAndDeletedAtIsNull(List.of(FILE_ID)))
                .thenReturn(List.of(file));

        service.deleteAllForProperty(
                PROPERTY_ID,
                ACTOR_CONTEXT,
                deletedAt
        );

        assertThat(image.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(file.getDeletedAt()).isEqualTo(deletedAt);
        verify(deletionPublisher).publishAfterCommit(List.of(file));
    }
}
