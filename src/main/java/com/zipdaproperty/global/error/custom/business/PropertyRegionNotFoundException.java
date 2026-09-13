package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class PropertyRegionNotFoundException extends BusinessException {

    public PropertyRegionNotFoundException(String message) {
        super(
                CustomResponseCode.PROPERTY_REGION_NOT_FOUND,
                message
        );
    }
}
