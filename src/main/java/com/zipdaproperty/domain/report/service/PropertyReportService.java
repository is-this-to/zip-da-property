package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.repository.PropertyReportRepository;
import com.zipdaproperty.domain.report.response.PropertyReportCreateResponse;
import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.id.TsidGenerator;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PropertyReportService {

    private static final long DAILY_REPORT_LIMIT = 5L;

    private static final String ACTIVE_REPORT_UNIQUE_CONSTRAINT =
            "uq_property_report_01";

    private static final String ACTIVE_REPORT_GENERATED_COLUMN =
            "active_report_key";

    private static final List<ReportStatus> ACTIVE_REPORT_STATUSES = List.of(
            ReportStatus.RECEIVED,
            ReportStatus.TRIAGED,
            ReportStatus.IN_REVIEW,
            ReportStatus.ACTIONED
    );

    private final PropertyReportRepository propertyReportRepository;
    private final PropertyRepository propertyRepository;
    private final TsidGenerator tsidGenerator;

    @Transactional
    public PropertyReportCreateResponse createReport(
            Long propertyId,
            ReportReasonCode reasonCode,
            String detail,
            ActorContext actorContext
    ) {
        validateReportActor(actorContext);

        propertyRepository.findByPropertyIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.PROPERTY_NOT_FOUND,
                        "신고할 수 있는 매물이 없습니다."
                ));

        Long reporterMemberId = actorContext.memberId();

        long dailyReportCount = propertyReportRepository
                .countDailyReportsIncludingDeleted(reporterMemberId);

        if (dailyReportCount >= DAILY_REPORT_LIMIT) {
            throw new BusinessException(
                    CustomResponseCode.RATE_LIMITED,
                    "하루 신고 가능 횟수를 초과했습니다."
            );
        }

        boolean hasActiveReport =
                propertyReportRepository
                        .existsByReporterMemberIdAndPropertyIdAndReasonCodeAndStatusInAndDeletedAtIsNull(
                                reporterMemberId,
                                propertyId,
                                reasonCode,
                                ACTIVE_REPORT_STATUSES
                        );

        if (hasActiveReport) {
            throw new BusinessException(
                    CustomResponseCode.DUPLICATE_ACTIVE_REPORT,
                    "동일한 사유의 진행 중 신고가 이미 존재합니다."
            );
        }

        PropertyReport propertyReport = PropertyReport.create(
                tsidGenerator.generate(),
                propertyId,
                reporterMemberId,
                reasonCode,
                detail,
                actorContext
        );

        PropertyReport savedReport = saveReport(propertyReport);

        return new PropertyReportCreateResponse(
                savedReport.getReportId(),
                savedReport.getStatus(),
                savedReport.getVersion()
        );
    }

    private PropertyReport saveReport(PropertyReport propertyReport) {
        try {
            return propertyReportRepository.saveAndFlush(propertyReport);
        } catch (DataIntegrityViolationException exception) {
            if (isActiveReportUniqueViolation(exception)) {
                throw new BusinessException(
                        CustomResponseCode.DUPLICATE_ACTIVE_REPORT,
                        "동일한 사유의 진행 중 신고가 이미 존재합니다."
                );
            }

            throw exception;
        }
    }

    private boolean isActiveReportUniqueViolation(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && isActiveReportConstraint(constraintViolation.getConstraintName())) {
                return true;
            }

            if (isActiveReportDuplicateMessage(cause.getMessage())) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private boolean isActiveReportConstraint(String constraintName) {
        return constraintName != null
                && ACTIVE_REPORT_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName);
    }

    private boolean isActiveReportDuplicateMessage(String message) {
        if (message == null) {
            return false;
        }

        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        boolean duplicateViolation = normalizedMessage.contains("duplicate entry")
                || normalizedMessage.contains("duplicate key")
                || normalizedMessage.contains("unique constraint")
                || normalizedMessage.contains("unique index");

        return duplicateViolation
                && (normalizedMessage.contains(ACTIVE_REPORT_UNIQUE_CONSTRAINT)
                || normalizedMessage.contains(ACTIVE_REPORT_GENERATED_COLUMN));
    }

    private void validateReportActor(ActorContext actorContext) {
        if (actorContext == null || !actorContext.isMemberRequest()) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "신고 기능을 사용할 수 없는 요청 주체입니다."
            );
        }

        boolean allowedRole = actorContext.role() == ActorRole.USER
                || actorContext.role() == ActorRole.AGENT;

        if (!allowedRole) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "신고 기능을 사용할 수 없는 요청 주체입니다."
            );
        }
    }
}
