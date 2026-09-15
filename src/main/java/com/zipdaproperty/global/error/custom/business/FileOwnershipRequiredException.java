package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class FileOwnershipRequiredException extends BusinessException {

    public FileOwnershipRequiredException(String message) {
        super(CustomResponseCode.FILE_OWNERSHIP_REQUIRED, message);
    }
}
