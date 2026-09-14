package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class PropertyReportAdminStatusTransitionPolicy {

    private static final Map<ReportStatus, Set<ReportStatus>> ALLOWED_TRANSITIONS =
            createAllowedTransitions();

    public void validate(ReportStatus currentStatus, ReportStatus targetStatus) {
        Set<ReportStatus> allowedTargets = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowedTargets == null || !allowedTargets.contains(targetStatus)) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REPORT_TRANSITION,
                    "허용되지 않은 신고 상태 전이입니다."
            );
        }
    }

    private static Map<ReportStatus, Set<ReportStatus>> createAllowedTransitions() {
        Map<ReportStatus, Set<ReportStatus>> transitions =
                new EnumMap<>(ReportStatus.class);
        transitions.put(
                ReportStatus.RECEIVED,
                EnumSet.of(ReportStatus.TRIAGED, ReportStatus.REJECTED)
        );
        transitions.put(
                ReportStatus.TRIAGED,
                EnumSet.of(ReportStatus.IN_REVIEW, ReportStatus.REJECTED)
        );
        transitions.put(
                ReportStatus.IN_REVIEW,
                EnumSet.of(ReportStatus.ACTIONED, ReportStatus.REJECTED)
        );
        transitions.put(ReportStatus.ACTIONED, EnumSet.of(ReportStatus.CLOSED));
        transitions.put(ReportStatus.REJECTED, EnumSet.noneOf(ReportStatus.class));
        transitions.put(ReportStatus.CLOSED, EnumSet.noneOf(ReportStatus.class));
        return Map.copyOf(transitions);
    }
}
