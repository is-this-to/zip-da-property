package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class OptionNotAllowedForPropertyTypeException extends BusinessException {

    public OptionNotAllowedForPropertyTypeException(String message) {
        super(CustomResponseCode.OPTION_NOT_ALLOWED_FOR_PROPERTY_TYPE, message);
    }
}
