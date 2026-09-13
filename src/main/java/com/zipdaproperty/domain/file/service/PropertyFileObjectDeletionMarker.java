package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.global.context.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PropertyFileObjectDeletionMarker {

    private final PropertyFileRepository propertyFileRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDeleted(Long propertyFileId, Instant deletedAt) {
        PropertyFile propertyFile = propertyFileRepository
                .findById(propertyFileId)
                .orElse(null);
        if (propertyFile == null
                || !propertyFile.isDeleted()
                || propertyFile.getObjectDeletedAt() != null) {
            return;
        }

        propertyFile.markObjectDeleted(
                deletedAt,
                ActorContext.system(
                        "property-file-object-delete-" + propertyFileId
                )
        );
    }
}
