package com.zipdaproperty.domain.image.service;

import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.service.PropertyFileObjectDeletionPublisher;
import com.zipdaproperty.domain.image.entity.PropertyImage;
import com.zipdaproperty.domain.image.repository.PropertyImageRepository;
import com.zipdaproperty.global.context.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyImageDeletionService {

    private static final String DELETE_REASON =
            "매물 삭제로 연결 이미지가 정리되었습니다.";

    private final PropertyImageRepository propertyImageRepository;
    private final PropertyFileRepository propertyFileRepository;
    private final PropertyFileObjectDeletionPublisher deletionPublisher;

    @Transactional
    public void deleteAllForProperty(
            Long propertyId,
            ActorContext actorContext,
            Instant deletedAt
    ) {
        List<PropertyImage> images = propertyImageRepository
                .findAllByPropertyIdAndDeletedAtIsNullOrderBySortOrderAsc(
                        propertyId
                );
        if (images.isEmpty()) {
            return;
        }

        List<PropertyFile> files = propertyFileRepository
                .findAllByPropertyFileIdInAndDeletedAtIsNull(
                        images.stream()
                                .map(PropertyImage::getPropertyFileId)
                                .toList()
                );
        for (PropertyImage image : images) {
            image.softDelete(actorContext, deletedAt, DELETE_REASON);
        }
        for (PropertyFile file : files) {
            file.softDelete(actorContext, deletedAt, DELETE_REASON);
        }
        deletionPublisher.publishAfterCommit(files);
    }
}
