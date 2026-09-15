package com.zipdaproperty.domain.file.event;

import java.util.List;

public record PropertyFileObjectDeletionRequested(
        List<Long> propertyFileIds
) {
    public PropertyFileObjectDeletionRequested {
        propertyFileIds = List.copyOf(propertyFileIds);
    }
}
