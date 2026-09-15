package com.zipdaproperty.domain.image.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.service.PropertyFileObjectDeletionPublisher;
import com.zipdaproperty.domain.image.entity.PropertyImage;
import com.zipdaproperty.domain.image.repository.PropertyImageRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.DuplicatedResourceException;
import com.zipdaproperty.global.error.custom.business.FileOwnershipRequiredException;
import com.zipdaproperty.global.error.custom.business.NotFoundResourceException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PropertyImageSyncService {

    private static final int MIN_IMAGE_COUNT = 1;
    private static final int MAX_IMAGE_COUNT = 30;
    private static final String DELETE_REASON =
            "매물 이미지 목록에서 제거되었습니다.";

    private final PropertyFileRepository propertyFileRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final PropertyFileObjectDeletionPublisher deletionPublisher;

    @Transactional(readOnly = true)
    public SyncPlan prepareSync(Long propertyId, List<Long> fileIds) {
        validateFileIds(fileIds);

        List<Long> currentFileIds = propertyImageRepository
                .findAllByPropertyIdAndDeletedAtIsNullOrderBySortOrderAsc(
                        propertyId
                )
                .stream()
                .map(PropertyImage::getPropertyFileId)
                .toList();

        return new SyncPlan(
                currentFileIds,
                !currentFileIds.equals(fileIds)
        );
    }

    @Transactional
    public void syncImages(
            Long propertyId,
            List<Long> fileIds,
            ActorContext actorContext
    ) {
        validateFileIds(fileIds);

        List<PropertyImage> existingImages = propertyImageRepository
                .findAllByPropertyIdAndDeletedAtIsNullOrderBySortOrderAsc(
                        propertyId
                );
        Map<Long, PropertyImage> existingByFileId = mapByFileId(existingImages);
        Set<Long> requestedFileIds = new HashSet<>(fileIds);

        List<PropertyImage> removedImages = existingImages.stream()
                .filter(image -> !requestedFileIds.contains(
                        image.getPropertyFileId()
                ))
                .toList();
        List<PropertyFile> removedFiles = findFilesToRemove(removedImages);
        Map<Long, PropertyFile> newFiles = findAndValidateNewFiles(
                fileIds,
                existingByFileId,
                actorContext
        );

        Long representativeFileId = fileIds.getFirst();
        PropertyImage currentRepresentative = existingImages.stream()
                .filter(PropertyImage::getIsRepresentative)
                .findFirst()
                .orElse(null);
        boolean representativeChanged = currentRepresentative == null
                || !representativeFileId.equals(
                        currentRepresentative.getPropertyFileId()
                );

        if (representativeChanged && currentRepresentative != null) {
            currentRepresentative.changeRepresentative(false, actorContext);
        }

        softDeleteRemoved(
                removedImages,
                removedFiles,
                actorContext
        );
        deletionPublisher.publishAfterCommit(removedFiles);

        if (representativeChanged && currentRepresentative != null) {
            propertyImageRepository.flush();
        }

        List<PropertyImage> newImages = synchronizeRequestedImages(
                propertyId,
                fileIds,
                existingByFileId,
                actorContext
        );

        if (!newImages.isEmpty()) {
            try {
                propertyImageRepository.saveAllAndFlush(newImages);
            } catch (DataIntegrityViolationException exception) {
                throw new DuplicatedResourceException(
                        "이미 다른 매물에 연결된 파일입니다."
                );
            }
        }

        for (PropertyFile propertyFile : newFiles.values()) {
            propertyFile.markLinked(actorContext);
        }
    }

    private Map<Long, PropertyImage> mapByFileId(
            List<PropertyImage> existingImages
    ) {
        Map<Long, PropertyImage> imagesByFileId = new LinkedHashMap<>();
        for (PropertyImage image : existingImages) {
            imagesByFileId.put(image.getPropertyFileId(), image);
        }
        return imagesByFileId;
    }

    private List<PropertyFile> findFilesToRemove(
            List<PropertyImage> removedImages
    ) {
        List<PropertyFile> removedFiles = new ArrayList<>();
        for (PropertyImage image : removedImages) {
            removedFiles.add(findActiveFile(image.getPropertyFileId()));
        }
        return removedFiles;
    }

    private Map<Long, PropertyFile> findAndValidateNewFiles(
            List<Long> fileIds,
            Map<Long, PropertyImage> existingByFileId,
            ActorContext actorContext
    ) {
        Map<Long, PropertyFile> newFiles = new LinkedHashMap<>();
        for (Long fileId : fileIds) {
            if (existingByFileId.containsKey(fileId)) {
                continue;
            }

            PropertyFile propertyFile = findActiveFile(fileId);
            validateOwnership(propertyFile, actorContext);
            validateFilePurpose(propertyFile);
            validateReadyToLink(propertyFile);
            newFiles.put(fileId, propertyFile);
        }
        return newFiles;
    }

    private List<PropertyImage> synchronizeRequestedImages(
            Long propertyId,
            List<Long> fileIds,
            Map<Long, PropertyImage> existingByFileId,
            ActorContext actorContext
    ) {
        List<PropertyImage> newImages = new ArrayList<>();
        for (int index = 0; index < fileIds.size(); index++) {
            Long fileId = fileIds.get(index);
            PropertyImage existingImage = existingByFileId.get(fileId);
            boolean representative = index == 0;

            if (existingImage != null) {
                existingImage.changeSortOrder(index, actorContext);
                existingImage.changeRepresentative(
                        representative,
                        actorContext
                );
                continue;
            }

            newImages.add(PropertyImage.create(
                    propertyId,
                    fileId,
                    index,
                    representative,
                    null,
                    actorContext
            ));
        }
        return newImages;
    }

    private void softDeleteRemoved(
            List<PropertyImage> removedImages,
            List<PropertyFile> removedFiles,
            ActorContext actorContext
    ) {
        Instant deletedAt = Instant.now();
        for (PropertyImage image : removedImages) {
            image.softDelete(actorContext, deletedAt, DELETE_REASON);
        }
        for (PropertyFile propertyFile : removedFiles) {
            propertyFile.softDelete(actorContext, deletedAt, DELETE_REASON);
        }
    }

    private PropertyFile findActiveFile(Long fileId) {
        return propertyFileRepository
                .findByPropertyFileIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new NotFoundResourceException(
                        "파일을 찾을 수 없습니다."
                ));
    }

    private void validateOwnership(
            PropertyFile propertyFile,
            ActorContext actorContext
    ) {
        if (actorContext == null
                || actorContext.memberId() == null
                || !propertyFile.getOwnerMemberId()
                .equals(actorContext.memberId())) {
            throw new FileOwnershipRequiredException(
                    "본인이 업로드한 파일만 매물 이미지로 연결할 수 있습니다."
            );
        }
    }

    private void validateFilePurpose(PropertyFile propertyFile) {
        if (propertyFile.getFilePurpose() != FilePurpose.PROPERTY_IMAGE) {
            throw invalidRequest(
                    "매물 이미지 용도로 업로드한 파일만 연결할 수 있습니다."
            );
        }
    }

    private void validateReadyToLink(PropertyFile propertyFile) {
        if (!propertyFile.isReadyToLink()) {
            throw invalidRequest(
                    "VERIFIED 상태의 파일만 매물 이미지로 연결할 수 있습니다."
            );
        }
    }

    private void validateFileIds(List<Long> fileIds) {
        if (fileIds == null
                || fileIds.size() < MIN_IMAGE_COUNT
                || fileIds.size() > MAX_IMAGE_COUNT) {
            throw invalidRequest(
                    "매물 이미지는 1개 이상 30개 이하로 등록해야 합니다."
            );
        }

        Set<Long> uniqueFileIds = new HashSet<>();
        for (Long fileId : fileIds) {
            if (fileId == null) {
                throw invalidRequest("파일 ID는 필수입니다.");
            }
            if (!uniqueFileIds.add(fileId)) {
                throw invalidRequest("중복된 파일 ID를 등록할 수 없습니다.");
            }
        }
    }

    private BusinessException invalidRequest(String message) {
        return new BusinessException(
                CustomResponseCode.INVALID_REQUEST,
                message
        );
    }

    public record SyncPlan(
            List<Long> currentFileIds,
            boolean changesRequired
    ) {
        public SyncPlan {
            currentFileIds = List.copyOf(currentFileIds);
        }
    }
}
