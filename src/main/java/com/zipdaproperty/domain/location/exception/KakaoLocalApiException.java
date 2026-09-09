package com.zipdaproperty.domain.location.exception;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class KakaoLocalApiException extends BusinessException {

    public KakaoLocalApiException(String message) {
        super(
                CustomResponseCode.KAKAO_LOCAL_API_ERROR,
                message
        );
    }
}
