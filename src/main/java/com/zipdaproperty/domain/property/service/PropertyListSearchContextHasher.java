package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.model.PropertyListBounds;
import com.zipdaproperty.domain.property.model.PropertyMapSearchCondition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class PropertyListSearchContextHasher {

    public String hash(PropertyListBounds bounds, PropertyMapSearchCondition condition) {
        String canonical = String.join("|",
                decimal(BigDecimal.valueOf(bounds.minLat())),
                decimal(BigDecimal.valueOf(bounds.maxLat())),
                decimal(BigDecimal.valueOf(bounds.minLng())),
                decimal(BigDecimal.valueOf(bounds.maxLng())),
                enumSet(condition.propertyTypes()),
                enumSet(condition.transactionTypes()),
                value(condition.minSalePrice()),
                value(condition.maxSalePrice()),
                value(condition.minDeposit()),
                value(condition.maxDeposit()),
                value(condition.minMonthlyRent()),
                value(condition.maxMonthlyRent()),
                value(condition.minMaintenanceFee()),
                value(condition.maxMaintenanceFee()),
                decimal(condition.minExclusiveArea()),
                decimal(condition.maxExclusiveArea()),
                value(condition.roomCountMin()),
                value(condition.roomCountMax()),
                enumSet(condition.publisherTypes()),
                value(condition.approvalDateFrom()),
                value(condition.approvalDateTo()),
                value(condition.isParkingAvailable()),
                value(condition.hasElevator()),
                value(condition.isPetAllowed()),
                condition.sort().name()
        );

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private String enumSet(Collection<? extends Enum<?>> values) {
        return values.stream()
                .map(Enum::name)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String decimal(BigDecimal value) {
        return value == null ? "~" : value.stripTrailingZeros().toPlainString();
    }

    private String value(Object value) {
        return value == null ? "~" : value.toString();
    }
}
