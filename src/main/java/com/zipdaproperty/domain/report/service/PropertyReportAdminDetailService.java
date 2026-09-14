package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.report.repository.PropertyReportAdminDetailQueryRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportAdminDetailQueryRow;
import com.zipdaproperty.domain.report.response.PropertyReportAdminDetailResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode.PROPERTY_REPORT_DETAIL_VIEWED;

@Service
@RequiredArgsConstructor
public class PropertyReportAdminDetailService {

    private static final String REPORT_AUDIT_TARGET_TYPE = "PROPERTY_REPORT";
    private static final int MAX_AUDIT_REASON_LENGTH = 200;

    private final PropertyReportAdminDetailQueryRepository propertyReportAdminDetailQueryRepository;
    private final PropertyAuditEventRecorder propertyAuditEventRecorder;

    @Transactional
    public PropertyReportAdminDetailResponse findReport(
            Long reportId,
            String auditReason,
            ActorContext actorContext
    ) {
        validateActor(actorContext);
        String normalizedAuditReason = validateAuditReason(auditReason);

        PropertyReportAdminDetailQueryRow row =
                propertyReportAdminDetailQueryRepository.findReport(reportId)
                        .orElseThrow(() -> new BusinessException(
                                CustomResponseCode.NOT_FOUND_RESOURCE,
                                "조회할 수 있는 신고가 없습니다."
                        ));

        propertyAuditEventRecorder.recordAction(
                REPORT_AUDIT_TARGET_TYPE,
                reportId.toString(),
                PROPERTY_REPORT_DETAIL_VIEWED,
                normalizedAuditReason,
                null,
                Instant.now(),
                actorContext
        );

        return toResponse(row);
    }

    private void validateActor(ActorContext actorContext) {
        if (actorContext == null || !actorContext.isMemberRequest()) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "관리자만 신고 상세를 조회할 수 있습니다."
            );
        }

        ActorRole role = actorContext.role();
        if (role != ActorRole.CS_ADMIN && role != ActorRole.SUPER_ADMIN) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "CS_ADMIN 또는 SUPER_ADMIN만 신고 상세를 조회할 수 있습니다."
            );
        }
    }

    private String validateAuditReason(String auditReason) {
        if (auditReason == null || auditReason.isBlank()) {
            throw new BusinessException(
                    CustomResponseCode.AUDIT_REASON_REQUIRED,
                    "감사 사유는 필수입니다."
            );
        }

        String normalizedAuditReason = auditReason.trim();
        if (normalizedAuditReason.length() > MAX_AUDIT_REASON_LENGTH) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "감사 사유는 200자 이하여야 합니다."
            );
        }

        return normalizedAuditReason;
    }

    private PropertyReportAdminDetailResponse toResponse(
            PropertyReportAdminDetailQueryRow row
    ) {
        return new PropertyReportAdminDetailResponse(
                row.reportId(),
                row.propertyId(),
                row.reporterMemberId(),
                row.reasonCode(),
                row.detail(),
                row.status(),
                row.riskScore(),
                row.assignedAdminId(),
                row.version(),
                row.createdAt()
        );
    }
}
