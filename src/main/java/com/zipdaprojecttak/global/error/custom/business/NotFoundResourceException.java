package com.zipdaprojecttak.global.error.custom.business;

import com.zipdaprojecttak.global.error.custom.BusinessException;
import com.zipdaprojecttak.global.response.constant.CustomResponseCode;

public class NotFoundResourceException extends BusinessException {

    public NotFoundResourceException(String message) {
        super(CustomResponseCode.NOT_FOUND_RESOURCE, message);
    }
}