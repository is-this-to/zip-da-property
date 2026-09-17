package com.zipdaproperty.domain.property.request;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PropertyPublicationAdminListRequest(
    PublicationStatus status,
    VerificationStatus verificationStatus,
    PublisherType publisherType,
    PropertyType propertyType,
    @Size(max = 300)
    String address,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate registeredFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate registeredTo,
    @Min(0)
    Integer page,
    @Min(1)
    @Max(50)
    Integer size
) {
    public PropertyPublicationAdminListRequest {
        if (status == null) status = PublicationStatus.IN_REVIEW;
        if (address != null) address = address.trim();
        if (page == null) page = 0;
        if (size == null) size = 20;
    }
}
