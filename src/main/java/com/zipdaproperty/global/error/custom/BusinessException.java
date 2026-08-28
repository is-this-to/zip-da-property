package com.zipdaproperty.global.error.custom;

import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final CustomResponseCode customResponseCode;

    public BusinessException(
            CustomResponseCode customResponseCode,
            String message
    ) {
        super(message);
        this.customResponseCode = customResponseCode;
    }
}