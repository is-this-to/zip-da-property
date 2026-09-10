package com.zipdaproperty.domain.image.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.image.entity.PropertyImage;
import com.zipdaproperty.domain.image.repository.PropertyImageRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.FileOwnershipRequiredException;
import com.zipdaproperty.global.error.custom.business.NotFoundResourceException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PropertyImageLinkService {

    private static final int MIN_IMAGE_COUNT = 1;
    private static final int MAX_IMAGE_COUNT = 30;

    private final PropertyFileRepository propertyFileRepository;
    private final PropertyImageRepository propertyImageRepository;

    @Transactional
    public void linkImages(
            Long propertyId,
            List<Long> fileIds,
            ActorContext actorContext
    ) {
        validateFileIds(fileIds);

        List<PropertyImage> images = new ArrayList<>();
        List<PropertyFile> propertyFiles = new ArrayList<>();

        for (int index = 0; index < fileIds.size(); index++) {
            Long fileId = fileIds.get(index);

            PropertyFile propertyFile =
                    findActiveFile(fileId);

            validateOwnership(
                    propertyFile,
                    actorContext
            );

            validateFilePurpose(propertyFile);

            validateReadyToLink(propertyFile);

            validateNotAlreadyLinked(
                    propertyId,
                    fileId
            );

            PropertyImage propertyImage =
                    PropertyImage.create(
                            propertyId,
                            fileId,
                            index,
                            index == 0,
                            null,
                            actorContext
                    );

            propertyFiles.add(propertyFile);
            images.add(propertyImage);
        }

        propertyImageRepository.saveAll(images);

        for (PropertyFile propertyFile : propertyFiles) {
            propertyFile.markLinked(actorContext);
        }
    }

    private void validateFileIds(
            List<Long> fileIds
    ) {
        if (fileIds == null
                || fileIds.size() < MIN_IMAGE_COUNT
                || fileIds.size() > MAX_IMAGE_COUNT) {

            throw invalidRequest(
                    "매물 이미지는 1개 이상 30개 이하로 등록해야 합니다."
            );
        }

        Set<Long> uniqueFileIds =
                new HashSet<>();

        for (Long fileId : fileIds) {
            if (fileId == null) {
                throw invalidRequest(
                        "파일 ID는 필수입니다."
                );
            }

            if (!uniqueFileIds.add(fileId)) {
                throw invalidRequest(
                        "중복된 파일 ID를 등록할 수 없습니다."
                );
            }
        }
    }

    private PropertyFile findActiveFile(
            Long fileId
    ) {
        return propertyFileRepository
                .findByPropertyFileIdAndDeletedAtIsNull(fileId)
                .orElseThrow(
                        () -> new NotFoundResourceException(
                                "파일을 찾을 수 없습니다."
                        )
                );
    }

    private void validateOwnership(
            PropertyFile propertyFile,
            ActorContext actorContext
    ) {
        if (actorContext == null
                || actorContext.memberId() == null
                || !propertyFile
                .getOwnerMemberId()
                .equals(actorContext.memberId())) {

            throw new FileOwnershipRequiredException(
                    "본인이 업로드한 파일만 매물 이미지로 연결할 수 있습니다."
            );
        }
    }

    private void validateFilePurpose(
            PropertyFile propertyFile
    ) {
        if (propertyFile.getFilePurpose()
                != FilePurpose.PROPERTY_IMAGE) {

            throw invalidRequest(
                    "매물 이미지 용도로 업로드한 파일만 연결할 수 있습니다."
            );
        }
    }

    private void validateReadyToLink(
            PropertyFile propertyFile
    ) {
        if (!propertyFile.isReadyToLink()) {
            throw invalidRequest(
                    "VERIFIED 상태의 파일만 매물 이미지로 연결할 수 있습니다."
            );
        }
    }

    private void validateNotAlreadyLinked(
            Long propertyId,
            Long fileId
    ) {
        boolean alreadyLinked =
                propertyImageRepository
                        .existsByPropertyIdAndPropertyFileIdAndDeletedAtIsNull(
                                propertyId,
                                fileId
                        );

        if (alreadyLinked) {
            throw new BusinessException(
                    CustomResponseCode.DUPLICATED_RESOURCE,
                    "이미 매물에 연결된 파일입니다."
            );
        }
    }

    private BusinessException invalidRequest(
            String message
    ) {
        return new BusinessException(
                CustomResponseCode.INVALID_REQUEST,
                message
        );
    }
}
