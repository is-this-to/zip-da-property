package com.zipdaproperty.domain.image.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.service.PropertyFileObjectDeletionPublisher;
import com.zipdaproperty.domain.image.entity.PropertyImage;
import com.zipdaproperty.domain.image.repository.PropertyImageRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.FileOwnershipRequiredException;
import com.zipdaproperty.global.error.custom.business.NotFoundResourceException;
import com.zipdaproperty.global.error.custom.business.DuplicatedResourceException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyImageSyncServiceTest {

    private static final long PROPERTY_ID = 100L;
    private static final long OWNER_MEMBER_ID = 200L;
    private static final ActorContext ACTOR_CONTEXT = ActorContext.member(
            OWNER_MEMBER_ID,
            ActorRole.USER,
            "property-image-sync-test"
    );

    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);
    private final PropertyImageRepository propertyImageRepository =
            mock(PropertyImageRepository.class);
    private final PropertyFileObjectDeletionPublisher deletionPublisher =
            mock(PropertyFileObjectDeletionPublisher.class);

    private PropertyImageSyncService service;

    @BeforeEach
    void setUp() {
        service = new PropertyImageSyncService(
                propertyFileRepository,
                propertyImageRepository,
                deletionPublisher
        );
    }

    @Test
    void syncImages_orderOnly_updatesContinuousSortOrder() {
        PropertyImage first = image(1L, 0, true);
        PropertyImage second = image(2L, 1, false);
        PropertyImage third = image(3L, 2, false);
        prepareExistingImages(first, second, third);

        service.syncImages(
                PROPERTY_ID,
                List.of(1L, 3L, 2L),
                ACTOR_CONTEXT
        );

        assertImage(first, 0, true);
        assertImage(third, 1, false);
        assertImage(second, 2, false);
        verify(propertyImageRepository, never()).flush();
        verify(propertyImageRepository, never()).saveAllAndFlush(anyList());
        verifyNoInteractions(propertyFileRepository);
    }

    @Test
    void prepareSync_sameImagesAndOrder_reportsNoChanges() {
        prepareExistingImages(
                image(1L, 0, true),
                image(2L, 1, false)
        );

        PropertyImageSyncService.SyncPlan syncPlan = service.prepareSync(
                PROPERTY_ID,
                List.of(1L, 2L)
        );

        assertThat(syncPlan.currentFileIds()).containsExactly(1L, 2L);
        assertThat(syncPlan.changesRequired()).isFalse();
    }

    @Test
    void prepareSync_changedOrder_reportsChanges() {
        prepareExistingImages(
                image(1L, 0, true),
                image(2L, 1, false)
        );

        PropertyImageSyncService.SyncPlan syncPlan = service.prepareSync(
                PROPERTY_ID,
                List.of(2L, 1L)
        );

        assertThat(syncPlan.currentFileIds()).containsExactly(1L, 2L);
        assertThat(syncPlan.changesRequired()).isTrue();
    }

    @Test
    void syncImages_representativeChange_demotesFlushesThenPromotes() {
        PropertyImage oldRepresentative = spy(image(1L, 0, true));
        PropertyImage newRepresentative = spy(image(2L, 1, false));
        prepareExistingImages(oldRepresentative, newRepresentative);

        service.syncImages(
                PROPERTY_ID,
                List.of(2L, 1L),
                ACTOR_CONTEXT
        );

        InOrder order = inOrder(
                oldRepresentative,
                propertyImageRepository,
                newRepresentative
        );
        order.verify(oldRepresentative)
                .changeRepresentative(false, ACTOR_CONTEXT);
        order.verify(propertyImageRepository).flush();
        order.verify(newRepresentative)
                .changeRepresentative(true, ACTOR_CONTEXT);
        assertImage(newRepresentative, 0, true);
        assertImage(oldRepresentative, 1, false);
    }

    @Test
    void syncImages_removeImage_softDeletesImageAndFile() {
        PropertyImage retained = image(1L, 0, true);
        PropertyImage removed = image(2L, 1, false);
        prepareExistingImages(retained, removed);
        PropertyFile removedFile = linkedFile(
                2L,
                OWNER_MEMBER_ID,
                FilePurpose.PROPERTY_IMAGE
        );
        Instant linkedAt = removedFile.getLinkedAt();
        prepareActiveFile(removedFile);

        service.syncImages(
                PROPERTY_ID,
                List.of(1L),
                ACTOR_CONTEXT
        );

        assertThat(removed.getDeletedAt()).isNotNull();
        assertThat(removed.getDeletedByMemberId())
                .isEqualTo(OWNER_MEMBER_ID);
        assertThat(removed.getDeletedByRole()).isEqualTo(ActorRole.USER);
        assertThat(removed.getDeleteReason()).isNotBlank();
        assertThat(removedFile.getDeletedAt()).isNotNull();
        assertThat(removedFile.getDeletedByMemberId())
                .isEqualTo(OWNER_MEMBER_ID);
        assertThat(removedFile.getDeletedByRole())
                .isEqualTo(ActorRole.USER);
        assertThat(removedFile.getDeleteReason()).isNotBlank();
        assertThat(removedFile.getUploadStatus())
                .isEqualTo(UploadStatus.LINKED);
        assertThat(removedFile.getLinkedAt()).isEqualTo(linkedAt);
        assertThat(removedFile.getObjectDeletedAt()).isNull();
        verify(deletionPublisher).publishAfterCommit(List.of(removedFile));
    }

    @Test
    void syncImages_addVerifiedFile_linksNewFileWithoutRelinkingRetainedFile() {
        PropertyImage retained = image(1L, 0, true);
        prepareExistingImages(retained);
        PropertyFile retainedFile = linkedFile(
                1L,
                OWNER_MEMBER_ID,
                FilePurpose.PROPERTY_IMAGE
        );
        Instant retainedLinkedAt = retainedFile.getLinkedAt();
        PropertyFile newFile = spy(verifiedFile(
                2L,
                OWNER_MEMBER_ID,
                FilePurpose.PROPERTY_IMAGE
        ));
        prepareActiveFile(newFile);

        service.syncImages(
                PROPERTY_ID,
                List.of(1L, 2L),
                ACTOR_CONTEXT
        );

        List<PropertyImage> newImages = captureSavedImages();
        assertThat(newImages).hasSize(1);
        assertThat(newImages.getFirst().getPropertyFileId()).isEqualTo(2L);
        assertImage(newImages.getFirst(), 1, false);
        assertThat(newFile.getUploadStatus()).isEqualTo(UploadStatus.LINKED);
        assertThat(newFile.getLinkedAt()).isNotNull();
        verify(newFile).markLinked(ACTOR_CONTEXT);
        verify(
                propertyFileRepository,
                never()
        ).findByPropertyFileIdAndDeletedAtIsNull(1L);
        assertThat(retainedFile.getUploadStatus())
                .isEqualTo(UploadStatus.LINKED);
        assertThat(retainedFile.getLinkedAt()).isEqualTo(retainedLinkedAt);
    }

    @Test
    void syncImages_activeFileUniqueConflict_translatesDuplicateException() {
        PropertyImage retained = image(1L, 0, true);
        prepareExistingImages(retained);
        PropertyFile newFile = verifiedFile(
                2L, OWNER_MEMBER_ID, FilePurpose.PROPERTY_IMAGE
        );
        prepareActiveFile(newFile);
        when(propertyImageRepository.saveAllAndFlush(anyList()))
                .thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> service.syncImages(
                PROPERTY_ID, List.of(1L, 2L), ACTOR_CONTEXT
        )).isInstanceOf(DuplicatedResourceException.class);

        assertThat(newFile.getUploadStatus()).isEqualTo(UploadStatus.VERIFIED);
    }

    @Test
    void syncImages_replaceImages_synchronizesDeletionAdditionOrderAndRepresentative() {
        PropertyImage first = image(1L, 0, true);
        PropertyImage second = image(2L, 1, false);
        PropertyImage removed = image(3L, 2, false);
        prepareExistingImages(first, second, removed);
        PropertyFile removedFile = linkedFile(
                3L,
                OWNER_MEMBER_ID,
                FilePurpose.PROPERTY_IMAGE
        );
        PropertyFile newFile = verifiedFile(
                4L,
                OWNER_MEMBER_ID,
                FilePurpose.PROPERTY_IMAGE
        );
        prepareActiveFile(removedFile);
        prepareActiveFile(newFile);

        service.syncImages(
                PROPERTY_ID,
                List.of(2L, 4L, 1L),
                ACTOR_CONTEXT
        );

        PropertyImage added = captureSavedImages().getFirst();
        List<PropertyImage> finalActiveImages = new ArrayList<>(
                List.of(first, second, added)
        );
        finalActiveImages.sort(Comparator.comparing(PropertyImage::getSortOrder));

        assertThat(removed.getDeletedAt()).isNotNull();
        assertThat(removedFile.getDeletedAt()).isNotNull();
        assertThat(newFile.getUploadStatus()).isEqualTo(UploadStatus.LINKED);
        assertThat(finalActiveImages)
                .extracting(PropertyImage::getSortOrder)
                .containsExactly(0, 1, 2);
        assertThat(finalActiveImages)
                .filteredOn(PropertyImage::getIsRepresentative)
                .singleElement()
                .extracting(PropertyImage::getPropertyFileId)
                .isEqualTo(2L);
    }

    @Test
    void syncImages_emptyFileIds_rejects() {
        assertInvalidRequest(List.of());
        verifyNoInteractions(
                propertyFileRepository,
                propertyImageRepository
        );
    }

    @Test
    void syncImages_nullFileIds_rejects() {
        assertInvalidRequest(null);
        verifyNoInteractions(
                propertyFileRepository,
                propertyImageRepository
        );
    }

    @Test
    void syncImages_thirtyOneFileIds_rejects() {
        List<Long> fileIds = new ArrayList<>();
        for (long fileId = 1; fileId <= 31; fileId++) {
            fileIds.add(fileId);
        }

        assertInvalidRequest(fileIds);
        verifyNoInteractions(
                propertyFileRepository,
                propertyImageRepository
        );
    }

    @Test
    void syncImages_duplicateFileId_rejects() {
        assertInvalidRequest(List.of(1L, 1L));
        verifyNoInteractions(
                propertyFileRepository,
                propertyImageRepository
        );
    }

    @Test
    void syncImages_nullFileId_rejects() {
        List<Long> fileIds = new ArrayList<>();
        fileIds.add(1L);
        fileIds.add(null);

        assertInvalidRequest(fileIds);
        verifyNoInteractions(
                propertyFileRepository,
                propertyImageRepository
        );
    }

    @Test
    void syncImages_missingOrSoftDeletedNewFile_rejects() {
        prepareExistingImages(image(1L, 0, true));
        when(propertyFileRepository.findByPropertyFileIdAndDeletedAtIsNull(2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.syncImages(
                PROPERTY_ID,
                List.of(1L, 2L),
                ACTOR_CONTEXT
        )).isInstanceOf(NotFoundResourceException.class);

        verify(propertyImageRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void syncImages_otherOwnerNewFile_rejects() {
        prepareExistingImages(image(1L, 0, true));
        prepareActiveFile(verifiedFile(
                2L,
                999L,
                FilePurpose.PROPERTY_IMAGE
        ));

        assertThatThrownBy(() -> service.syncImages(
                PROPERTY_ID,
                List.of(1L, 2L),
                ACTOR_CONTEXT
        )).isInstanceOf(FileOwnershipRequiredException.class);

        verify(propertyImageRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void syncImages_nonPropertyImagePurposeNewFile_rejects() {
        prepareExistingImages(image(1L, 0, true));
        prepareActiveFile(verifiedFile(
                2L,
                OWNER_MEMBER_ID,
                FilePurpose.VERIFICATION
        ));

        assertInvalidRequest(List.of(1L, 2L));
        verify(propertyImageRepository, never()).saveAllAndFlush(anyList());
    }

    @ParameterizedTest
    @EnumSource(
            value = UploadStatus.class,
            names = {"CREATED", "LINKED"}
    )
    void syncImages_nonVerifiedNewFile_rejects(UploadStatus uploadStatus) {
        prepareExistingImages(image(1L, 0, true));
        PropertyFile propertyFile = fileWithStatus(2L, uploadStatus);
        prepareActiveFile(propertyFile);

        assertInvalidRequest(List.of(1L, 2L));

        assertThat(propertyFile.getUploadStatus()).isEqualTo(uploadStatus);
        verify(propertyImageRepository, never()).saveAllAndFlush(anyList());
    }

    private void prepareExistingImages(PropertyImage... images) {
        when(propertyImageRepository
                .findAllByPropertyIdAndDeletedAtIsNullOrderBySortOrderAsc(
                        PROPERTY_ID
                ))
                .thenReturn(List.of(images));
    }

    private void prepareActiveFile(PropertyFile propertyFile) {
        when(propertyFileRepository.findByPropertyFileIdAndDeletedAtIsNull(
                propertyFile.getPropertyFileId()
        )).thenReturn(Optional.of(propertyFile));
    }

    private PropertyImage image(
            Long fileId,
            int sortOrder,
            boolean representative
    ) {
        return PropertyImage.create(
                PROPERTY_ID,
                fileId,
                sortOrder,
                representative,
                null,
                ACTOR_CONTEXT
        );
    }

    private PropertyFile verifiedFile(
            Long fileId,
            Long ownerMemberId,
            FilePurpose filePurpose
    ) {
        ActorContext ownerContext = ownerContext(ownerMemberId, fileId);
        PropertyFile propertyFile = PropertyFile.create(
                fileId,
                "upload-session-" + fileId,
                filePurpose,
                "photo-" + fileId + ".jpg",
                1024L,
                "opaque-test-value-" + fileId,
                Instant.now().plusSeconds(900),
                ownerContext
        );
        propertyFile.complete(
                "a".repeat(64),
                "image/jpg",
                ownerContext
        );
        return propertyFile;
    }

    private PropertyFile linkedFile(
            Long fileId,
            Long ownerMemberId,
            FilePurpose filePurpose
    ) {
        PropertyFile propertyFile = verifiedFile(
                fileId,
                ownerMemberId,
                filePurpose
        );
        propertyFile.markLinked(ownerContext(ownerMemberId, fileId));
        return propertyFile;
    }

    private PropertyFile fileWithStatus(
            Long fileId,
            UploadStatus uploadStatus
    ) {
        return switch (uploadStatus) {
            case CREATED -> PropertyFile.create(
                    fileId,
                    "upload-session-" + fileId,
                    FilePurpose.PROPERTY_IMAGE,
                    "photo-" + fileId + ".jpg",
                    1024L,
                    "opaque-test-value-" + fileId,
                    Instant.now().plusSeconds(900),
                    ownerContext(OWNER_MEMBER_ID, fileId)
            );
            case LINKED -> linkedFile(
                    fileId,
                    OWNER_MEMBER_ID,
                    FilePurpose.PROPERTY_IMAGE
            );
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 테스트 상태입니다."
            );
        };
    }

    private ActorContext ownerContext(Long ownerMemberId, Long fileId) {
        return ActorContext.member(
                ownerMemberId,
                ActorRole.USER,
                "property-file-test-" + fileId
        );
    }

    @SuppressWarnings("unchecked")
    private List<PropertyImage> captureSavedImages() {
        ArgumentCaptor<List<PropertyImage>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(propertyImageRepository).saveAllAndFlush(captor.capture());
        return captor.getValue();
    }

    private void assertImage(
            PropertyImage image,
            int sortOrder,
            boolean representative
    ) {
        assertThat(image.getSortOrder()).isEqualTo(sortOrder);
        assertThat(image.getIsRepresentative()).isEqualTo(representative);
    }

    private void assertInvalidRequest(List<Long> fileIds) {
        assertThatThrownBy(() -> service.syncImages(
                PROPERTY_ID,
                fileIds,
                ACTOR_CONTEXT
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.INVALID_REQUEST)
        );
    }
}
