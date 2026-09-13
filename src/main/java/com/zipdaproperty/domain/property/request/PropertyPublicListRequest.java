package com.zipdaproperty.domain.property.request;

import com.zipdaproperty.domain.property.constant.PropertyMapSort;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.model.PropertyListBounds;
import com.zipdaproperty.domain.property.model.PropertyMapSearchCondition;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record PropertyPublicListRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double minLat,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double maxLat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double minLng,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double maxLng,
        @Size(max = 4) Set<@NotNull PropertyType> propertyTypes,
        @Size(max = 3) Set<@NotNull TransactionType> transactionTypes,
        @PositiveOrZero Long minSalePrice,
        @PositiveOrZero Long maxSalePrice,
        @PositiveOrZero Long minDeposit,
        @PositiveOrZero Long maxDeposit,
        @PositiveOrZero Long minMonthlyRent,
        @PositiveOrZero Long maxMonthlyRent,
        @PositiveOrZero Long minMaintenanceFee,
        @PositiveOrZero Long maxMaintenanceFee,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal minExclusiveArea,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal maxExclusiveArea,
        @Min(1) Integer roomCountMin,
        @Min(1) Integer roomCountMax,
        @Size(max = 3) Set<@NotNull PublisherType> publisherTypes,
        LocalDate approvalDateFrom,
        LocalDate approvalDateTo,
        Boolean isParkingAvailable,
        Boolean hasElevator,
        Boolean isPetAllowed,
        PropertyMapSort sort,
        @Size(min = 1, max = 1000) String cursor,
        @Min(1) @Max(50) Integer size
) {

    private static final int DEFAULT_SIZE = 20;

    public PropertyPublicListRequest {
        if (size == null) {
            size = DEFAULT_SIZE;
        }
    }

    public PropertyListBounds toBounds() {
        return new PropertyListBounds(minLat, maxLat, minLng, maxLng);
    }

    public PropertyMapSearchCondition toSearchCondition() {
        return new PropertyMapSearchCondition(
                propertyTypes,
                transactionTypes,
                minSalePrice,
                maxSalePrice,
                minDeposit,
                maxDeposit,
                minMonthlyRent,
                maxMonthlyRent,
                minMaintenanceFee,
                maxMaintenanceFee,
                minExclusiveArea,
                maxExclusiveArea,
                roomCountMin,
                roomCountMax,
                publisherTypes,
                approvalDateFrom,
                approvalDateTo,
                isParkingAvailable,
                hasElevator,
                isPetAllowed,
                sort
        );
    }
}
