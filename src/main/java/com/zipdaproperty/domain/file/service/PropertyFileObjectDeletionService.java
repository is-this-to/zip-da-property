package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.storage.MinioObjectDeleter;
import com.zipdaproperty.global.context.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PropertyFileObjectDeletionService {

    private final PropertyFileRepository propertyFileRepository;
    private final MinioObjectDeleter minioObjectDeleter;
    private final PropertyFileObjectDeletionMarker deletionMarker;

    public void deleteObject(Long propertyFileId) {
        PropertyFile propertyFile = propertyFileRepository
                .findById(propertyFileId)
                .orElse(null);
        if (propertyFile == null
                || !propertyFile.isDeleted()
                || propertyFile.getObjectDeletedAt() != null) {
            return;
        }

        minioObjectDeleter.delete(propertyFile.getObjectKey());
        deletionMarker.markDeleted(propertyFileId, Instant.now());
    }
}
