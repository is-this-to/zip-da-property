package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class UploadSessionExpiredException extends BusinessException {

    public UploadSessionExpiredException(String message) {
        super(CustomResponseCode.UPLOAD_SESSION_EXPIRED, message);
    }
}
