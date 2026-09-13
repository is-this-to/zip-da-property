package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.entity.PropertyReportAppeal;
import com.zipdaproperty.domain.report.repository.PropertyReportAppealRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportRepository;
import com.zipdaproperty.domain.report.response.PropertyReportAppealCreateResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PropertyReportAppealService {

    private final PropertyReportRepository propertyReportRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyReportAppealRepository propertyReportAppealRepository;

    @Transactional
    public PropertyReportAppealCreateResponse createAppeal(
            Long reportId,
            String detail,
            ActorContext actorContext
    ) {
        validateActor(actorContext);

        PropertyReport report = propertyReportRepository
                .findByReportIdAndDeletedAtIsNull(reportId)
                .orElseThrow(this::reportNotFound);

        Property property = propertyRepository
                .findByPropertyIdAndDeletedAtIsNull(report.getPropertyId())
                .orElseThrow(this::propertyNotFound);

        if (!property.getAuthorMemberId().equals(actorContext.memberId())) {
            throw ownershipRequired();
        }

        PropertyReportAppeal appeal = PropertyReportAppeal.create(
                report.getReportId(),
                actorContext.memberId(),
                detail,
                actorContext
        );
        propertyReportAppealRepository.save(appeal);

        return new PropertyReportAppealCreateResponse(
                report.getReportId(),
                report.getStatus(),
                report.getVersion()
        );
    }

    private void validateActor(ActorContext actorContext) {
        if (actorContext == null || !actorContext.isMemberRequest()) {
            throw forbidden();
        }

        ActorRole role = actorContext.role();
        if (role != ActorRole.USER && role != ActorRole.AGENT) {
            throw forbidden();
        }
    }

    private BusinessException reportNotFound() {
        return new BusinessException(
                CustomResponseCode.NOT_FOUND_RESOURCE,
                "조회할 수 있는 신고가 없습니다."
        );
    }

    private BusinessException propertyNotFound() {
        return new BusinessException(
                CustomResponseCode.PROPERTY_NOT_FOUND,
                "이의신청 대상 매물이 없습니다."
        );
    }

    private BusinessException ownershipRequired() {
        return new BusinessException(
                CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
                "매물 작성자만 이의신청할 수 있습니다."
        );
    }

    private BusinessException forbidden() {
        return new BusinessException(
                CustomResponseCode.FORBIDDEN,
                "USER 또는 AGENT만 이의신청할 수 있습니다."
        );
    }
}
