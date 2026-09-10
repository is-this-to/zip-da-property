package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class PropertyLocationRegionMismatchException extends BusinessException {

    public PropertyLocationRegionMismatchException(String message) {
        super(
                CustomResponseCode.PROPERTY_LOCATION_REGION_MISMATCH,
                message
        );
    }
}
