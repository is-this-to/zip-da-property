package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.report.entity.PropertyReportAction;
import com.zipdaproperty.domain.report.entity.PropertyReportAppeal;
import com.zipdaproperty.domain.report.repository.PropertyReportActionRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportAppealRepository;
import com.zipdaproperty.domain.report.request.PropertyReportAppealAdminReviewRequest;
import com.zipdaproperty.domain.report.response.PropertyReportAppealAdminReviewResponse;
import com.zipdaproperty.domain.report.type.AppealStatus;
import com.zipdaproperty.domain.report.type.ReportActionCode;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyReportAppealAdminReviewService {

    private static final List<ReportActionCode> ALLOWED_ACTION_CODES = List.of(
            ReportActionCode.HIDE_PROPERTY,
            ReportActionCode.REQUEST_CORRECTION
    );

    private final PropertyReportAppealRepository propertyReportAppealRepository;
    private final PropertyReportActionRepository propertyReportActionRepository;
    private final PropertyReportAppealAdminTransitionPolicy transitionPolicy;

    @Transactional
    public PropertyReportAppealAdminReviewResponse review(
            Long appealId,
            PropertyReportAppealAdminReviewRequest request,
            ActorContext actorContext
    ) {
        validateActor(actorContext);

        PropertyReportAppeal appeal = propertyReportAppealRepository
                .findByAppealIdAndDeletedAtIsNull(appealId)
                .orElseThrow(this::appealNotFound);

        validateVersion(appeal.getVersion(), request.version());
        transitionPolicy.validate(appeal.getStatus(), request.targetStatus());

        PropertyReportAction action = propertyReportActionRepository
                .findFirstByReportIdAndActionCodeInOrderByExecutedAtDescActionIdDesc(
                        appeal.getReportId(),
                        ALLOWED_ACTION_CODES
                )
                .orElseThrow(this::appealNotAllowed);

        if (action.getActorMemberId().equals(actorContext.memberId())) {
            throw selfReviewForbidden();
        }

        if (request.targetStatus() == AppealStatus.IN_REVIEW) {
            appeal.startReview(actorContext);
        } else {
            appeal.completeReview(
                    request.targetStatus(),
                    request.reviewReason(),
                    Instant.now(),
                    actorContext
            );
        }

        return PropertyReportAppealAdminReviewResponse.from(
                saveAndFlush(appeal)
        );
    }

    private PropertyReportAppeal saveAndFlush(PropertyReportAppeal appeal) {
        try {
            return propertyReportAppealRepository.saveAndFlush(appeal);
        } catch (OptimisticLockingFailureException exception) {
            throw new BusinessException(
                    CustomResponseCode.VERSION_CONFLICT,
                    "다른 관리자가 먼저 이의신청을 처리했습니다."
            );
        }
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
                    "이의신청 version이 일치하지 않습니다."
            );
        }
    }

    private BusinessException appealNotFound() {
        return new BusinessException(
                CustomResponseCode.NOT_FOUND_RESOURCE,
                "조회할 수 있는 이의신청이 없습니다."
        );
    }

    private BusinessException appealNotAllowed() {
        return new BusinessException(
                CustomResponseCode.APPEAL_NOT_ALLOWED,
                "이의신청 기준 운영조치가 없습니다."
        );
    }

    private BusinessException selfReviewForbidden() {
        return new BusinessException(
                CustomResponseCode.FORBIDDEN,
                "운영조치 실행자는 동일 이의신청을 재검토할 수 없습니다."
        );
    }

    private BusinessException forbidden() {
        return new BusinessException(
                CustomResponseCode.FORBIDDEN,
                "CS_ADMIN 또는 SUPER_ADMIN만 이의신청을 처리할 수 있습니다."
        );
    }
}
