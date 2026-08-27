package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class UnauthenticatedException extends BusinessException {

    public UnauthenticatedException(String message) {
        super(CustomResponseCode.UNAUTHENTICATED, message);
    }
}