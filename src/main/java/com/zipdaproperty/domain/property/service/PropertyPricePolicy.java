package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

@Component
public class PropertyPricePolicy {

    public void validate(
            TransactionType transactionType,
            Long salePrice,
            Long deposit,
            Long monthlyRent
    ) {
        if (transactionType == null) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PRICE_COMBINATION,
                    "거래 유형은 필수입니다."
            );
        }

        boolean isValid = switch (transactionType) {
            case SALE -> isSalePriceValid(
                    salePrice,
                    deposit,
                    monthlyRent
            );
            case JEONSE -> isJeonsePriceValid(
                    salePrice,
                    deposit,
                    monthlyRent
            );
            case MONTHLY_RENT -> isMonthlyRentPriceValid(
                    salePrice,
                    deposit,
                    monthlyRent
            );
        };

        if (!isValid) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PRICE_COMBINATION,
                    "거래 유형에 맞지 않는 가격 조합입니다."
            );
        }
    }

    private boolean isSalePriceValid(
            Long salePrice,
            Long deposit,
            Long monthlyRent
    ) {
        return isPositive(salePrice)
                && deposit == null
                && monthlyRent == null;
    }

    private boolean isJeonsePriceValid(
            Long salePrice,
            Long deposit,
            Long monthlyRent
    ) {
        return salePrice == null
                && isPositive(deposit)
                && monthlyRent == null;
    }

    private boolean isMonthlyRentPriceValid(
            Long salePrice,
            Long deposit,
            Long monthlyRent
    ) {
        return salePrice == null
                && isZeroOrPositive(deposit)
                && isPositive(monthlyRent);
    }

    private boolean isPositive(Long amount) {
        return amount != null && amount > 0;
    }

    private boolean isZeroOrPositive(Long amount) {
        return amount != null && amount >= 0;
    }
}