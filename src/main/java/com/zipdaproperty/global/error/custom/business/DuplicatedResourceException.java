package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class DuplicatedResourceException extends BusinessException {

    public DuplicatedResourceException(String message) {
        super(CustomResponseCode.DUPLICATED_RESOURCE, message);
    }
}