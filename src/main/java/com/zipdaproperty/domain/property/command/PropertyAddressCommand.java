package com.zipdaproperty.domain.property.command;

import java.math.BigDecimal;

public record PropertyAddressCommand(
        String roadAddress,
        String jibunAddress,
        String legalDongCode,
        BigDecimal longitude,
        BigDecimal latitude
) {
}
