package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.repository.PropertyReportRepository;
import com.zipdaproperty.domain.report.request.PropertyReportAdminStatusChangeRequest;
import com.zipdaproperty.domain.report.response.PropertyReportAdminStatusChangeResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode.PROPERTY_REPORT_STATUS_CHANGED;

@Service
@RequiredArgsConstructor
public class PropertyReportAdminStatusChangeService {

    private static final String REPORT_AUDIT_TARGET_TYPE = "PROPERTY_REPORT";

    private final PropertyReportRepository propertyReportRepository;
    private final PropertyReportAdminStatusTransitionPolicy transitionPolicy;
    private final PropertyAuditEventRecorder propertyAuditEventRecorder;

    @Transactional
    public PropertyReportAdminStatusChangeResponse changeStatus(
            Long reportId,
            PropertyReportAdminStatusChangeRequest request,
            ActorContext actorContext
    ) {
        validateActor(actorContext);

        PropertyReport report = propertyReportRepository
                .findByReportIdAndDeletedAtIsNull(reportId)
                .orElseThrow(this::reportNotFound);

        validateVersion(report.getVersion(), request.version());
        transitionPolicy.validate(report.getStatus(), request.targetStatus());

        Instant occurredAt = Instant.now();
        report.changeStatus(request.targetStatus(), actorContext);
        PropertyReport savedReport = saveAndFlush(report);

        propertyAuditEventRecorder.recordAction(
                REPORT_AUDIT_TARGET_TYPE,
                savedReport.getReportId().toString(),
                PROPERTY_REPORT_STATUS_CHANGED,
                null,
                null,
                occurredAt,
                actorContext
        );

        return PropertyReportAdminStatusChangeResponse.from(savedReport);
    }

    private void validateActor(ActorContext actorContext) {
        if (actorContext == null || !actorContext.isMemberRequest()) {
            throw forbidden();
        }

        ActorRole role = actorContext.role();
        if (role != ActorRole.CS_ADMIN && role != ActorRole.SUPER_ADMIN) {
            throw forbidden();
        }
    }

    private void validateVersion(Long currentVersion, Long requestedVersion) {
        if (requestedVersion == null || !requestedVersion.equals(currentVersion)) {
            throw new BusinessException(
                    CustomResponseCode.VERSION_CONFLICT,
                    "신고 version이 일치하지 않습니다."
            );
        }
    }

    private PropertyReport saveAndFlush(PropertyReport report) {
        try {
            return propertyReportRepository.saveAndFlush(report);
        } catch (OptimisticLockingFailureException exception) {
            throw new BusinessException(
                    CustomResponseCode.VERSION_CONFLICT,
                    "다른 관리자가 먼저 신고 상태를 변경했습니다."
            );
        }
    }

    private BusinessException reportNotFound() {
        return new BusinessException(
                CustomResponseCode.NOT_FOUND_RESOURCE,
                "조회할 수 있는 신고가 없습니다."
        );
    }

    private BusinessException forbidden() {
        return new BusinessException(
                CustomResponseCode.FORBIDDEN,
                "CS_ADMIN 또는 SUPER_ADMIN만 신고 상태를 변경할 수 있습니다."
        );
    }
}
