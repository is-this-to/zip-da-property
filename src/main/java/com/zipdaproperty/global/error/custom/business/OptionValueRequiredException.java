package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class OptionValueRequiredException extends BusinessException {

    public OptionValueRequiredException(String message) {
        super(CustomResponseCode.OPTION_VALUE_REQUIRED, message);
    }
}
