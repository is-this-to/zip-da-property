package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.global.id.TsidString;

public record PropertyCreateResponse(

        @TsidString
        Long propertyId,

        Long version,

        PublicationStatus publicationStatus,

        TransactionStatus transactionStatus,

        VerificationStatus verificationStatus

) {

    public static PropertyCreateResponse from(Property property) {
        return new PropertyCreateResponse(
                property.getPropertyId(),
                property.getVersion(),
                property.getPublicationStatus(),
                property.getTransactionStatus(),
                property.getVerificationStatus()
        );
    }
}