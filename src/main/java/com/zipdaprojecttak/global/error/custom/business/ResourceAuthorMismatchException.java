package com.zipdaprojecttak.global.error.custom.business;

import com.zipdaprojecttak.global.error.custom.BusinessException;
import com.zipdaprojecttak.global.response.constant.CustomResponseCode;

public class ResourceAuthorMismatchException extends BusinessException {

    public ResourceAuthorMismatchException(String message) {
        super(CustomResponseCode.RESOURCE_AUTHOR_MISMATCH_ERROR, message);
    }
}