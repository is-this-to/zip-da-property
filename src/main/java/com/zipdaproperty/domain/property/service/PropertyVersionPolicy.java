package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

@Component
public class PropertyVersionPolicy {

    public void validate(
            Long currentVersion,
            Long requestedVersion
    ) {
        if (requestedVersion == null) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "요청 version은 필수입니다."
            );
        }

        if (!requestedVersion.equals(currentVersion)) {
            throw new BusinessException(
                    CustomResponseCode.VERSION_CONFLICT,
                    "매물 version이 일치하지 않습니다."
            );
        }
    }
}