package com.zipdaproperty.domain.file.exception;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class FileTooLargeException extends BusinessException {

    public FileTooLargeException(String message) {
        super(CustomResponseCode.FILE_TOO_LARGE, message);
    }
}
