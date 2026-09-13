package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.entity.PropertyReportAction;
import com.zipdaproperty.domain.report.entity.PropertyReportAppeal;
import com.zipdaproperty.domain.report.repository.PropertyReportActionRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportAppealRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportRepository;
import com.zipdaproperty.domain.report.response.PropertyReportAppealCreateResponse;
import com.zipdaproperty.domain.report.type.ReportActionCode;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PropertyReportAppealService {

    private static final String APPEAL_UNIQUE_CONSTRAINT =
            "uq_property_report_appeal_report";
    private static final List<ReportActionCode> ALLOWED_ACTION_CODES = List.of(
            ReportActionCode.HIDE_PROPERTY,
            ReportActionCode.REQUEST_CORRECTION
    );

    private final PropertyReportRepository propertyReportRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyReportAppealRepository propertyReportAppealRepository;
    private final PropertyReportActionRepository propertyReportActionRepository;

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

        PropertyReportAction action = propertyReportActionRepository
                .findFirstByReportIdAndActionCodeInOrderByExecutedAtDescActionIdDesc(
                        reportId,
                        ALLOWED_ACTION_CODES
                )
                .orElseThrow(this::appealNotAllowed);

        if (action.getExecutedAt().isBefore(
                Instant.now().minus(7, ChronoUnit.DAYS)
        )) {
            throw appealPeriodExpired();
        }

        if (propertyReportAppealRepository.existsByReportId(reportId)) {
            throw duplicateAppeal();
        }

        PropertyReportAppeal appeal = PropertyReportAppeal.create(
                report.getReportId(),
                actorContext.memberId(),
                detail,
                actorContext
        );
        saveAndFlush(appeal);

        return new PropertyReportAppealCreateResponse(
                report.getReportId(),
                report.getStatus(),
                report.getVersion()
        );
    }

    private void saveAndFlush(PropertyReportAppeal appeal) {
        try {
            propertyReportAppealRepository.saveAndFlush(appeal);
        } catch (DataIntegrityViolationException exception) {
            if (isAppealUniqueViolation(exception)) {
                throw duplicateAppeal();
            }
            throw exception;
        }
    }

    private boolean isAppealUniqueViolation(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null
                    && message.toLowerCase(Locale.ROOT).contains(
                    APPEAL_UNIQUE_CONSTRAINT
            )) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
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

    private BusinessException duplicateAppeal() {
        return new BusinessException(
                CustomResponseCode.DUPLICATE_APPEAL,
                "이미 이의신청이 접수된 신고입니다."
        );
    }

    private BusinessException appealNotAllowed() {
        return new BusinessException(
                CustomResponseCode.APPEAL_NOT_ALLOWED,
                "이의신청할 수 있는 운영조치가 없습니다."
        );
    }

    private BusinessException appealPeriodExpired() {
        return new BusinessException(
                CustomResponseCode.APPEAL_PERIOD_EXPIRED,
                "이의신청 가능 기간이 지났습니다."
        );
    }
}
