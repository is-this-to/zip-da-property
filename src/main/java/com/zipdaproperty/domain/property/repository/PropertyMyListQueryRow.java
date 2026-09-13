package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;

import java.time.Instant;

public record PropertyMyListQueryRow(

        Long propertyId,

        Long version,

        Long regionId,

        String title,

        PropertyType propertyType,

        TransactionType transactionType,

        Long salePrice,

        Long deposit,

        Long monthlyRent,

        PublisherType publisherType,

        PublicationStatus publicationStatus,

        TransactionStatus transactionStatus,

        VerificationStatus verificationStatus,

        Instant updatedAt

) {
}