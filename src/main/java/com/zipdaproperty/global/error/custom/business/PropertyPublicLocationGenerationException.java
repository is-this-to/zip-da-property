package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class PropertyPublicLocationGenerationException
        extends BusinessException {

    public PropertyPublicLocationGenerationException(String message) {
        super(
                CustomResponseCode.PROPERTY_PUBLIC_LOCATION_GENERATION_FAILED,
                message
        );
    }
}
