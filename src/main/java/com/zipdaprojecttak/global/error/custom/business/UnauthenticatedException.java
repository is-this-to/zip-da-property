package com.zipdaprojecttak.global.error.custom.business;

import com.zipdaprojecttak.global.error.custom.BusinessException;
import com.zipdaprojecttak.global.response.constant.CustomResponseCode;

public class UnauthenticatedException extends BusinessException {

    public UnauthenticatedException(String message) {
        super(CustomResponseCode.UNAUTHENTICATED, message);
    }
}