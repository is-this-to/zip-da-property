package com.zipdaproperty.global.error.custom.business;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

public class PropertyRegionBoundaryNotFoundException extends BusinessException {

    public PropertyRegionBoundaryNotFoundException(String message) {
        super(
                CustomResponseCode.PROPERTY_REGION_BOUNDARY_NOT_FOUND,
                message
        );
    }
}
