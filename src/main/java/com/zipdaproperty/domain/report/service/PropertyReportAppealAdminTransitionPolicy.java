package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.report.type.AppealStatus;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class PropertyReportAppealAdminTransitionPolicy {

    private static final Map<AppealStatus, Set<AppealStatus>> TRANSITIONS =
            createTransitions();

    public void validate(AppealStatus current, AppealStatus target) {
        Set<AppealStatus> targets = TRANSITIONS.get(current);
        if (targets == null || !targets.contains(target)) {
            throw new BusinessException(
                    CustomResponseCode.APPEAL_NOT_ALLOWED,
                    "허용되지 않은 이의신청 상태 전이입니다."
            );
        }
    }

    private static Map<AppealStatus, Set<AppealStatus>> createTransitions() {
        Map<AppealStatus, Set<AppealStatus>> transitions =
                new EnumMap<>(AppealStatus.class);
        transitions.put(
                AppealStatus.SUBMITTED,
                EnumSet.of(AppealStatus.IN_REVIEW)
        );
        transitions.put(
                AppealStatus.IN_REVIEW,
                EnumSet.of(AppealStatus.ACCEPTED, AppealStatus.REJECTED)
        );
        transitions.put(
                AppealStatus.ACCEPTED,
                EnumSet.noneOf(AppealStatus.class)
        );
        transitions.put(
                AppealStatus.REJECTED,
                EnumSet.noneOf(AppealStatus.class)
        );
        return Map.copyOf(transitions);
    }
}
