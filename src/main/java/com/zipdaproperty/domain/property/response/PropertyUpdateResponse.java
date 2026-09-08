package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.global.id.TsidString;

public record PropertyUpdateResponse(

        @TsidString
        Long propertyId,

        Long version,

        PublicationStatus publicationStatus,

        TransactionStatus transactionStatus,

        VerificationStatus verificationStatus

) {

    public static PropertyUpdateResponse from(
            Property property
    ) {
        return new PropertyUpdateResponse(
                property.getPropertyId(),
                property.getVersion(),
                property.getPublicationStatus(),
                property.getTransactionStatus(),
                property.getVerificationStatus()
        );
    }
}