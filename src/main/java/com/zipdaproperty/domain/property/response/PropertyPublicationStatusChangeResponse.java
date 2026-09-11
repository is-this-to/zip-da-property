package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.global.id.TsidString;

import java.time.Instant;

public record PropertyPublicationStatusChangeResponse(

        @TsidString
        Long propertyId,

        Long version,

        PublicationStatus publicationStatus,

        TransactionStatus transactionStatus,

        VerificationStatus verificationStatus,

        Instant publishedAt

) {

    public static PropertyPublicationStatusChangeResponse from(
            Property property
    ) {
        return new PropertyPublicationStatusChangeResponse(
                property.getPropertyId(),
                property.getVersion(),
                property.getPublicationStatus(),
                property.getTransactionStatus(),
                property.getVerificationStatus(),
                property.getPublishedAt()
        );
    }
}
