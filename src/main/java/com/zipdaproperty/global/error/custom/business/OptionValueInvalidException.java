package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class OptionValueInvalidException extends BusinessException {

    public OptionValueInvalidException(String message) {
        super(CustomResponseCode.OPTION_VALUE_INVALID, message);
    }
}
