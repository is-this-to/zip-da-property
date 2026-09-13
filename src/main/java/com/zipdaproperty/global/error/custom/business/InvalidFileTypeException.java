package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class InvalidFileTypeException extends BusinessException {

    public InvalidFileTypeException(String message) {
        super(CustomResponseCode.INVALID_FILE_TYPE, message);
    }
}
