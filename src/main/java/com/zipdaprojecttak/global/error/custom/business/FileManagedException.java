package com.zipdaprojecttak.global.error.custom.business;

import com.zipdaprojecttak.global.error.custom.BusinessException;
import com.zipdaprojecttak.global.response.constant.CustomResponseCode;

public class FileManagedException extends BusinessException {

    public FileManagedException(String message) {
        super(CustomResponseCode.FILE_MANAGED_ERROR, message);
    }
}