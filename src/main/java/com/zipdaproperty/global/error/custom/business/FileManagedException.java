package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class FileManagedException extends BusinessException {

    public FileManagedException(String message) {
        super(CustomResponseCode.FILE_MANAGED_ERROR, message);
    }
}