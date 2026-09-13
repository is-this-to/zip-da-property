package com.zipdaproperty.domain.property.command;

import com.zipdaproperty.domain.property.constant.PublicationStatus;

public record PropertyPublicationStatusChangeCommand(
        Long propertyId,
        PublicationStatus targetStatus,
        String reason
) {
}
