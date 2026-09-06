package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class OptionCodeNotFoundException extends BusinessException {

    public OptionCodeNotFoundException(String message) {
        super(CustomResponseCode.OPTION_CODE_NOT_FOUND, message);
    }
}
