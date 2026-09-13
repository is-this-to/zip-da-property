package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.property.command.PropertyPublicationStatusChangeCommand;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.service.PropertyPublicationStatusChangeService;
import com.zipdaproperty.domain.report.client.MemberSanctionClient;
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
import org.springframework.beans.factory.ObjectProvider;
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
    private final PropertyRepository propertyRepository;
    private final ObjectProvider<MemberSanctionClient>
            memberSanctionClientProvider;

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

        executePropertyAction(
                report.getPropertyId(),
                request.actionCode(),
                request.reason(),
                actorContext
        );

        Instant executedAt = Instant.now();

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
            case REQUEST_MEMBER_SANCTION -> requestMemberSanction(
                    propertyId,
                    reason
            );
        }
    }

    private void requestMemberSanction(
            Long propertyId,
            String reason
    ) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(this::propertyNotFound);

        MemberSanctionClient memberSanctionClient =
                memberSanctionClientProvider.getIfAvailable();
        if (memberSanctionClient == null) {
            throw memberApiUnavailable();
        }

        memberSanctionClient.requestSanction(
                property.getAuthorMemberId(),
                reason
        );
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

    private BusinessException propertyNotFound() {
        return new BusinessException(
                CustomResponseCode.PROPERTY_NOT_FOUND,
                "제재 대상을 확인할 매물이 없습니다."
        );
    }

    private BusinessException memberApiUnavailable() {
        return new BusinessException(
                CustomResponseCode.MEMBER_API_UNAVAILABLE,
                "Member 제재 연동을 사용할 수 없습니다."
        );
    }
}
