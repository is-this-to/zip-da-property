package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class ResourceAuthorMismatchException extends BusinessException {

    public ResourceAuthorMismatchException(String message) {
        super(CustomResponseCode.RESOURCE_AUTHOR_MISMATCH_ERROR, message);
    }
}