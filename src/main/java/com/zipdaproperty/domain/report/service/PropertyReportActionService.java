package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.property.command.PropertyPublicationStatusChangeCommand;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.service.PropertyPublicationStatusChangeService;
import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.entity.PropertyReportAction;
import com.zipdaproperty.domain.report.repository.PropertyReportActionRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportRepository;
import com.zipdaproperty.domain.report.request.PropertyReportActionRequest;
import com.zipdaproperty.domain.report.response.PropertyReportActionResponse;
import com.zipdaproperty.domain.report.type.ReportActionCode;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PropertyReportActionService {

    private final PropertyReportRepository propertyReportRepository;
    private final PropertyReportActionRepository
            propertyReportActionRepository;
    private final PropertyPublicationStatusChangeService
            propertyPublicationStatusChangeService;

    @Transactional
    public PropertyReportActionResponse executeAction(
            Long reportId,
            PropertyReportActionRequest request,
            ActorContext actorContext
    ) {
        validateActor(actorContext);

        PropertyReport report = propertyReportRepository
                .findByReportIdAndDeletedAtIsNull(reportId)
                .orElseThrow(this::reportNotFound);

        Instant executedAt = Instant.now();

        executePropertyAction(
                report.getPropertyId(),
                request.actionCode(),
                request.reason(),
                actorContext
        );

        PropertyReportAction action = PropertyReportAction.create(
                reportId,
                report.getPropertyId(),
                request.actionCode(),
                request.reason(),
                executedAt,
                actorContext
        );

        PropertyReportAction savedAction =
                propertyReportActionRepository.save(action);

        return PropertyReportActionResponse.from(savedAction);
    }

    private void executePropertyAction(
            Long propertyId,
            ReportActionCode actionCode,
            String reason,
            ActorContext actorContext
    ) {
        switch (actionCode) {
            case HIDE_PROPERTY -> changePublicationStatus(
                    propertyId,
                    PublicationStatus.HIDDEN,
                    reason,
                    actorContext
            );
            case RESTORE_PROPERTY -> changePublicationStatus(
                    propertyId,
                    PublicationStatus.PUBLISHED,
                    reason,
                    actorContext
            );
            case REQUEST_CORRECTION -> {
                // Property 상태를 변경하지 않고 조치 이력만 기록한다.
            }
            case REQUEST_MEMBER_SANCTION -> throw unsupportedAction();
        }
    }

    private void changePublicationStatus(
            Long propertyId,
            PublicationStatus targetStatus,
            String reason,
            ActorContext actorContext
    ) {
        propertyPublicationStatusChangeService.change(
                new PropertyPublicationStatusChangeCommand(
                        propertyId,
                        targetStatus,
                        reason
                ),
                actorContext
        );
    }

    private void validateActor(ActorContext actorContext) {
        if (actorContext == null || !actorContext.isMemberRequest()) {
            throw forbidden();
        }

        ActorRole role = actorContext.role();
        if (role != ActorRole.CS_ADMIN
                && role != ActorRole.SUPER_ADMIN) {
            throw forbidden();
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
                "CS_ADMIN 또는 SUPER_ADMIN만 신고 운영조치를 실행할 수 있습니다."
        );
    }

    private BusinessException unsupportedAction() {
        return new BusinessException(
                CustomResponseCode.INVALID_REQUEST,
                "이번 단계에서 지원하지 않는 신고 운영조치입니다."
        );
    }
}
