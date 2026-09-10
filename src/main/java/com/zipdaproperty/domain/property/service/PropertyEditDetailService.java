package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.response.PropertyEditDetailResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PropertyEditDetailService {

    private final PropertyRepository propertyRepository;

    @Transactional(readOnly = true)
    public PropertyEditDetailResponse getEditDetail(
            Long propertyId,
            ActorContext actorContext
    ) {
        Property property = findActiveProperty(propertyId);

        validateEditPermission(
                property,
                actorContext
        );

        return PropertyEditDetailResponse.from(property);
    }

    private Property findActiveProperty(Long propertyId) {
        if (propertyId == null || propertyId <= 0) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_NOT_FOUND,
                    "매물을 찾을 수 없습니다."
            );
        }

        return propertyRepository
                .findByPropertyIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(
                        () -> new BusinessException(
                                CustomResponseCode.PROPERTY_NOT_FOUND,
                                "매물을 찾을 수 없습니다. propertyId = "
                                        + propertyId
                        )
                );
    }

    private void validateEditPermission(
            Property property,
            ActorContext actorContext
    ) {
        if (actorContext == null
                || !actorContext.isMemberRequest()) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
                    "회원 요청만 매물 수정용 상세 정보를 조회할 수 있습니다."
            );
        }

        ActorRole actorRole = actorContext.role();

        boolean isAuthor =
                (
                        actorRole == ActorRole.USER
                                || actorRole == ActorRole.AGENT
                )
                        && Objects.equals(
                        property.getAuthorMemberId(),
                        actorContext.memberId()
                );

        boolean isAllowedAdmin =
                actorRole == ActorRole.CS_ADMIN
                        || actorRole == ActorRole.SUPER_ADMIN;

        if (!isAuthor && !isAllowedAdmin) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
                    "매물 작성자 또는 허용된 관리자만 수정용 상세 정보를 조회할 수 있습니다."
            );
        }
    }
}