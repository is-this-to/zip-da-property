package com.zipdaproperty.domain.image.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.image.entity.PropertyImage;
import com.zipdaproperty.domain.image.repository.PropertyImageRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.FileOwnershipRequiredException;
import com.zipdaproperty.global.error.custom.business.NotFoundResourceException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyImageLinkServiceTest {

    private static final long PROPERTY_ID = 100L;
    private static final long OWNER_MEMBER_ID = 200L;

    private static final ActorContext ACTOR_CONTEXT = ActorContext.member(
            OWNER_MEMBER_ID,
            ActorRole.USER,
            "property-image-link-test"
    );

    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);

    private final PropertyImageRepository propertyImageRepository =
            mock(PropertyImageRepository.class);

    private PropertyImageLinkService service;

    @BeforeEach
    void setUp() {
        service = new PropertyImageLinkService(
                propertyFileRepository,
                propertyImageRepository
        );
    }

    @Test
    void linkImages_singleFile_createsRepresentativeAtSortOrderZero() {
        Long fileId = 1_001L;

        prepareActiveCompletedFile(
                fileId,
                OWNER_MEMBER_ID
        );

        service.linkImages(
                PROPERTY_ID,
                List.of(fileId),
                ACTOR_CONTEXT
        );

        List<PropertyImage> savedImages =
                captureSavedImages();

        assertThat(savedImages).hasSize(1);

        assertImage(
                savedImages.getFirst(),
                PROPERTY_ID,
                fileId,
                0,
                true
        );
    }

    @Test
    void linkImages_multipleFiles_preservesOrderAndFirstIsRepresentative() {
        List<Long> fileIds =
                List.of(
                        1_001L,
                        1_002L,
                        1_003L
                );

        fileIds.forEach(
                fileId ->
                        prepareActiveCompletedFile(
                                fileId,
                                OWNER_MEMBER_ID
                        )
        );

        service.linkImages(
                PROPERTY_ID,
                fileIds,
                ACTOR_CONTEXT
        );

        List<PropertyImage> savedImages =
                captureSavedImages();

        assertThat(savedImages).hasSize(3);

        assertImage(
                savedImages.get(0),
                PROPERTY_ID,
                fileIds.get(0),
                0,
                true
        );

        assertImage(
                savedImages.get(1),
                PROPERTY_ID,
                fileIds.get(1),
                1,
                false
        );

        assertImage(
                savedImages.get(2),
                PROPERTY_ID,
                fileIds.get(2),
                2,
                false
        );
    }

    @Test
    void linkImages_nullFileIds_rejects() {
        assertInvalidRequest(
                () -> service.linkImages(
                        PROPERTY_ID,
                        null,
                        ACTOR_CONTEXT
                )
        );

        verifyNoInteractions(
                propertyFileRepository,
                propertyImageRepository
        );
    }

    @Test
    void linkImages_emptyFileIds_rejects() {
        assertInvalidRequest(
                () -> service.linkImages(
                        PROPERTY_ID,
                        List.of(),
                        ACTOR_CONTEXT
                )
        );

        verifyNoInteractions(
                propertyFileRepository,
                propertyImageRepository
        );
    }

    @Test
    void linkImages_thirtyOneFiles_rejects() {
        List<Long> fileIds =
                new ArrayList<>();

        for (long fileId = 1; fileId <= 31; fileId++) {
            fileIds.add(fileId);
        }

        assertInvalidRequest(
                () -> service.linkImages(
                        PROPERTY_ID,
                        fileIds,
                        ACTOR_CONTEXT
                )
        );

        verifyNoInteractions(
                propertyFileRepository,
                propertyImageRepository
        );
    }

    @Test
    void linkImages_nullFileId_rejects() {
        List<Long> fileIds =
                new ArrayList<>();

        fileIds.add(1_001L);
        fileIds.add(null);

        assertInvalidRequest(
                () -> service.linkImages(
                        PROPERTY_ID,
                        fileIds,
                        ACTOR_CONTEXT
                )
        );

        verifyNoInteractions(
                propertyFileRepository,
                propertyImageRepository
        );
    }

    @Test
    void linkImages_duplicateFileIdInRequest_rejects() {
        assertInvalidRequest(
                () -> service.linkImages(
                        PROPERTY_ID,
                        List.of(
                                1_001L,
                                1_001L
                        ),
                        ACTOR_CONTEXT
                )
        );

        verifyNoInteractions(
                propertyFileRepository,
                propertyImageRepository
        );
    }

    @Test
    void linkImages_missingOrSoftDeletedFile_rejects() {
        Long fileId = 1_001L;

        when(
                propertyFileRepository
                        .findByPropertyFileIdAndDeletedAtIsNull(
                                fileId
                        )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.linkImages(
                        PROPERTY_ID,
                        List.of(fileId),
                        ACTOR_CONTEXT
                )
        ).isInstanceOf(
                NotFoundResourceException.class
        );

        verify(
                propertyImageRepository,
                never()
        ).saveAll(anyList());
    }

    @Test
    void linkImages_otherOwner_rejectsWithFileOwnershipRequired() {
        Long fileId = 1_001L;

        prepareActiveCompletedFile(
                fileId,
                999L
        );

        assertThatThrownBy(
                () -> service.linkImages(
                        PROPERTY_ID,
                        List.of(fileId),
                        ACTOR_CONTEXT
                )
        )
                .isInstanceOf(
                        FileOwnershipRequiredException.class
                )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(
                                        exception
                                                .getCustomResponseCode()
                                ).isEqualTo(
                                        CustomResponseCode
                                                .FILE_OWNERSHIP_REQUIRED
                                )
                );

        verify(
                propertyImageRepository,
                never()
        ).saveAll(anyList());
    }

    @Test
    void linkImages_incompleteFile_rejects() {
        Long fileId = 1_001L;

        PropertyFile propertyFile =
                propertyFile(
                        fileId,
                        OWNER_MEMBER_ID,
                        false
                );

        when(
                propertyFileRepository
                        .findByPropertyFileIdAndDeletedAtIsNull(
                                fileId
                        )
        ).thenReturn(
                Optional.of(propertyFile)
        );

        assertInvalidRequest(
                () -> service.linkImages(
                        PROPERTY_ID,
                        List.of(fileId),
                        ACTOR_CONTEXT
                )
        );

        verify(
                propertyImageRepository,
                never()
        ).saveAll(anyList());
    }

    @Test
    void linkImages_existingActivePropertyFileLink_rejectsAsDuplicate() {
        Long fileId = 1_001L;

        prepareActiveCompletedFile(
                fileId,
                OWNER_MEMBER_ID
        );

        when(
                propertyImageRepository
                        .existsByPropertyIdAndPropertyFileIdAndDeletedAtIsNull(
                                PROPERTY_ID,
                                fileId
                        )
        ).thenReturn(true);

        assertThatThrownBy(
                () -> service.linkImages(
                        PROPERTY_ID,
                        List.of(fileId),
                        ACTOR_CONTEXT
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception
                                        .getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode
                                        .DUPLICATED_RESOURCE
                        )
        );

        verify(
                propertyImageRepository,
                never()
        ).saveAll(anyList());
    }

    @Test
    void linkImages_oneInvalidFileAmongMany_doesNotSaveAnyImage() {
        Long validFileId = 1_001L;
        Long incompleteFileId = 1_002L;

        prepareActiveCompletedFile(
                validFileId,
                OWNER_MEMBER_ID
        );

        when(
                propertyFileRepository
                        .findByPropertyFileIdAndDeletedAtIsNull(
                                incompleteFileId
                        )
        ).thenReturn(
                Optional.of(
                        propertyFile(
                                incompleteFileId,
                                OWNER_MEMBER_ID,
                                false
                        )
                )
        );

        assertInvalidRequest(
                () -> service.linkImages(
                        PROPERTY_ID,
                        List.of(
                                validFileId,
                                incompleteFileId
                        ),
                        ACTOR_CONTEXT
                )
        );

        verify(
                propertyImageRepository,
                never()
        ).saveAll(anyList());
    }

    @Test
    void linkImages_doesNotApplyGlobalFileDuplicatePolicy() {
        Long fileId = 1_001L;

        prepareActiveCompletedFile(
                fileId,
                OWNER_MEMBER_ID
        );

        service.linkImages(
                999L,
                List.of(fileId),
                ACTOR_CONTEXT
        );

        verify(
                propertyImageRepository,
                never()
        ).existsByPropertyFileIdAndDeletedAtIsNull(
                fileId
        );

        assertImage(
                captureSavedImages().getFirst(),
                999L,
                fileId,
                0,
                true
        );
    }

    private void prepareActiveCompletedFile(
            Long fileId,
            Long ownerMemberId
    ) {
        when(
                propertyFileRepository
                        .findByPropertyFileIdAndDeletedAtIsNull(
                                fileId
                        )
        ).thenReturn(
                Optional.of(
                        propertyFile(
                                fileId,
                                ownerMemberId,
                                true
                        )
                )
        );
    }

    private PropertyFile propertyFile(
            Long fileId,
            Long ownerMemberId,
            boolean completed
    ) {
        ActorContext ownerContext =
                ActorContext.member(
                        ownerMemberId,
                        ActorRole.USER,
                        "property-file-test-" + fileId
                );

        PropertyFile propertyFile =
                PropertyFile.create(
                        fileId,
                        "upload-session-" + fileId,
                        FilePurpose.PROPERTY_IMAGE,
                        "photo-" + fileId + ".jpg",
                        1024L,
                        "property-files/" + fileId + ".jpg",
                        Instant.now().plusSeconds(900),
                        ownerContext
                );

        if (completed) {
            propertyFile.complete(
                    "a".repeat(64),
                    "image/jpeg",
                    ownerContext
            );
        }

        return propertyFile;
    }

    @SuppressWarnings("unchecked")
    private List<PropertyImage> captureSavedImages() {
        ArgumentCaptor<List<PropertyImage>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(
                propertyImageRepository
        ).saveAll(
                captor.capture()
        );

        return captor.getValue();
    }

    private void assertImage(
            PropertyImage image,
            Long propertyId,
            Long propertyFileId,
            int sortOrder,
            boolean representative
    ) {
        assertThat(
                image.getPropertyId()
        ).isEqualTo(propertyId);

        assertThat(
                image.getPropertyFileId()
        ).isEqualTo(propertyFileId);

        assertThat(
                image.getSortOrder()
        ).isEqualTo(sortOrder);

        assertThat(
                image.getIsRepresentative()
        ).isEqualTo(representative);
    }

    private void assertInvalidRequest(
            Runnable invocation
    ) {
        assertThatThrownBy(
                invocation::run
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception
                                        .getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode
                                        .INVALID_REQUEST
                        )
        );
    }
}