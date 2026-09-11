package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.event.PropertyFileObjectDeletionRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PropertyFileObjectDeletionPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishAfterCommit(List<PropertyFile> propertyFiles) {
        if (propertyFiles.isEmpty()) {
            return;
        }

        applicationEventPublisher.publishEvent(
                new PropertyFileObjectDeletionRequested(
                        propertyFiles.stream()
                                .map(PropertyFile::getPropertyFileId)
                                .toList()
                )
        );
    }
}
