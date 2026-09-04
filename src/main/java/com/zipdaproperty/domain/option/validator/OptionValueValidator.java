package com.zipdaproperty.domain.option.validator;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

@Component
public class OptionValueValidator {

    public void validate(String optionValue) {
        if (optionValue == null || optionValue.isBlank()) {
            throw new BusinessException(
                    CustomResponseCode.OPTION_VALUE_REQUIRED,
                    "옵션 값이 필요합니다."
            );
        }

        if (!"있음".equals(optionValue) && !"없음".equals(optionValue)) {
            throw new BusinessException(
                    CustomResponseCode.OPTION_VALUE_TYPE_MISMATCH,
                    "옵션 값은 있음 또는 없음이어야 합니다."
            );
        }
    }
}
