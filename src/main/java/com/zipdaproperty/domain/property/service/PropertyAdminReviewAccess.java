package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

@Component
public class PropertyAdminReviewAccess {
    public void requireAdmin(ActorContext actor) {
        if (actor == null || !actor.isMemberRequest()
                || (actor.role() != ActorRole.CS_ADMIN && actor.role() != ActorRole.SUPER_ADMIN)) {
            throw new BusinessException(CustomResponseCode.FORBIDDEN, "관리자만 검수 정보를 조회할 수 있습니다.");
        }
    }

    public String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(CustomResponseCode.AUDIT_REASON_REQUIRED, "감사 사유는 필수입니다.");
        }
        String normalized = reason.trim();
        if (normalized.length() > 200) {
            throw new BusinessException(CustomResponseCode.INVALID_REQUEST, "감사 사유는 200자 이하여야 합니다.");
        }
        return normalized;
    }
}
