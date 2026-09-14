package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.FileOwnershipRequiredException;
import com.zipdaproperty.global.error.custom.business.NotFoundResourceException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyFileDeleteServiceTest {

    private static final Long FILE_ID = 101L;
    private static final Long OWNER_ID = 1001L;
    private static final ActorContext OWNER_CONTEXT = ActorContext.member(
            OWNER_ID,
            ActorRole.USER,
            "property-file-delete-test"
    );

    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);
    private final PropertyFileObjectDeletionPublisher deletionPublisher =
            mock(PropertyFileObjectDeletionPublisher.class);

    private PropertyFileDeleteService service;
    private PropertyFile propertyFile;

    @BeforeEach
    void setUp() {
        service = new PropertyFileDeleteService(
                propertyFileRepository,
                deletionPublisher
        );
        propertyFile = verifiedFile();
        when(propertyFileRepository.findByPropertyFileIdAndDeletedAtIsNull(
                FILE_ID
        )).thenReturn(Optional.of(propertyFile));
    }

    @Test
    void delete_ownedUnlinkedFile_softDeletesAndPublishesObjectDeletion() {
        service.delete(FILE_ID, OWNER_CONTEXT);

        assertThat(propertyFile.isDeleted()).isTrue();
        assertThat(propertyFile.getDeletedAt()).isNotNull();
        verify(deletionPublisher).publishAfterCommit(List.of(propertyFile));
    }

    @Test
    void delete_fileOwnedByAnotherMember_throwsFileOwnershipRequired() {
        ActorContext anotherMember = ActorContext.member(
                2002L,
                ActorRole.USER,
                "property-file-delete-other-member-test"
        );

        assertThatThrownBy(() -> service.delete(FILE_ID, anotherMember))
                .isInstanceOf(FileOwnershipRequiredException.class);

        assertThat(propertyFile.isDeleted()).isFalse();
        verify(deletionPublisher, never()).publishAfterCommit(List.of(propertyFile));
    }

    @Test
    void delete_linkedFile_throwsInvalidRequest() {
        propertyFile.markLinked(OWNER_CONTEXT);

        assertThatThrownBy(() -> service.delete(FILE_ID, OWNER_CONTEXT))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.INVALID_REQUEST)
                );

        assertThat(propertyFile.isDeleted()).isFalse();
        verify(deletionPublisher, never()).publishAfterCommit(List.of(propertyFile));
    }

    @Test
    void delete_missingFile_throwsNotFoundResource() {
        when(propertyFileRepository.findByPropertyFileIdAndDeletedAtIsNull(
                FILE_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(FILE_ID, OWNER_CONTEXT))
                .isInstanceOf(NotFoundResourceException.class);

        verify(deletionPublisher, never()).publishAfterCommit(List.of(propertyFile));
    }

    private PropertyFile verifiedFile() {
        PropertyFile file = PropertyFile.create(
                FILE_ID,
                "upload-session",
                FilePurpose.PROPERTY_IMAGE,
                "photo.jpg",
                1024L,
                "property-files/test/101.jpg",
                Instant.now().plusSeconds(900),
                OWNER_CONTEXT
        );
        file.complete(
                "a".repeat(64),
                "image/jpeg",
                OWNER_CONTEXT
        );
        return file;
    }
}
