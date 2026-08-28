package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class NotFoundResourceException extends BusinessException {

    public NotFoundResourceException(String message) {
        super(CustomResponseCode.NOT_FOUND_RESOURCE, message);
    }
}