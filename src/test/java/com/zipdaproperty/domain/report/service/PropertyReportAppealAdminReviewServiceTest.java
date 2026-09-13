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
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyReportAppealAdminReviewServiceTest {

    private static final Long APPEAL_ID = 41L;
    private static final Long REPORT_ID = 884685586571263701L;
    private static final Long ACTION_ADMIN_ID = 3001L;
    private static final Long REVIEW_ADMIN_ID = 3002L;

    private final PropertyReportAppealRepository appealRepository =
            mock(PropertyReportAppealRepository.class);
    private final PropertyReportActionRepository actionRepository =
            mock(PropertyReportActionRepository.class);
    private final PropertyReportAppealAdminTransitionPolicy transitionPolicy =
            new PropertyReportAppealAdminTransitionPolicy();
    private final PropertyReportAppealAdminReviewService service =
            new PropertyReportAppealAdminReviewService(
                    appealRepository,
                    actionRepository,
                    transitionPolicy
            );

    @Test
    void review_submittedToInReview_recordsReviewerOnly() {
        PropertyReportAppeal appeal = prepareAppeal(AppealStatus.SUBMITTED, 0L);
        prepareLatestAllowedAction(ACTION_ADMIN_ID);
        when(appealRepository.saveAndFlush(appeal)).thenReturn(appeal);

        PropertyReportAppealAdminReviewResponse response = service.review(
                APPEAL_ID,
                request(AppealStatus.IN_REVIEW, null, 0L),
                admin(REVIEW_ADMIN_ID)
        );

        assertThat(appeal.getStatus()).isEqualTo(AppealStatus.IN_REVIEW);
        assertThat(appeal.getReviewerMemberId()).isEqualTo(REVIEW_ADMIN_ID);
        assertThat(appeal.getReviewedAt()).isNull();
        assertThat(appeal.getReviewReason()).isNull();
        assertThat(response.appealId()).isEqualTo(APPEAL_ID);
        assertThat(response.status()).isEqualTo(AppealStatus.IN_REVIEW);
        assertThat(response.version()).isEqualTo(0L);
    }

    @Test
    void review_inReviewToAccepted_recordsFinalReview() {
        PropertyReportAppeal appeal = prepareAppeal(AppealStatus.IN_REVIEW, 1L);
        prepareLatestAllowedAction(ACTION_ADMIN_ID);
        when(appealRepository.saveAndFlush(appeal)).thenReturn(appeal);

        service.review(
                APPEAL_ID,
                request(AppealStatus.ACCEPTED, "승인 사유", 1L),
                admin(REVIEW_ADMIN_ID)
        );

        assertThat(appeal.getStatus()).isEqualTo(AppealStatus.ACCEPTED);
        assertThat(appeal.getReviewerMemberId()).isEqualTo(REVIEW_ADMIN_ID);
        assertThat(appeal.getReviewReason()).isEqualTo("승인 사유");
        assertThat(appeal.getReviewedAt()).isNotNull();
    }

    @Test
    void review_sameAdminAsLatestAllowedAction_throwsForbidden() {
        PropertyReportAppeal appeal = prepareAppeal(AppealStatus.SUBMITTED, 0L);
        prepareLatestAllowedAction(ACTION_ADMIN_ID);

        assertThatThrownBy(() -> service.review(
                APPEAL_ID,
                request(AppealStatus.IN_REVIEW, null, 0L),
                admin(ACTION_ADMIN_ID)
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.FORBIDDEN)
        );

        verify(appealRepository, never()).saveAndFlush(any());
        assertThat(appeal.getStatus()).isEqualTo(AppealStatus.SUBMITTED);
    }

    @Test
    void review_versionMismatch_throwsVersionConflict() {
        prepareAppeal(AppealStatus.SUBMITTED, 2L);

        assertThatThrownBy(() -> service.review(
                APPEAL_ID,
                request(AppealStatus.IN_REVIEW, null, 1L),
                admin(REVIEW_ADMIN_ID)
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.VERSION_CONFLICT)
        );

        verify(actionRepository, never())
                .findFirstByReportIdAndActionCodeInOrderByExecutedAtDescActionIdDesc(
                        any(),
                        any()
                );
    }

    @Test
    void review_optimisticLockFailure_throwsVersionConflict() {
        PropertyReportAppeal appeal = prepareAppeal(AppealStatus.SUBMITTED, 0L);
        prepareLatestAllowedAction(ACTION_ADMIN_ID);
        when(appealRepository.saveAndFlush(appeal)).thenThrow(
                new OptimisticLockingFailureException("conflict")
        );

        assertThatThrownBy(() -> service.review(
                APPEAL_ID,
                request(AppealStatus.IN_REVIEW, null, 0L),
                admin(REVIEW_ADMIN_ID)
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.VERSION_CONFLICT)
        );
    }

    private PropertyReportAppeal prepareAppeal(
            AppealStatus status,
            Long version
    ) {
        PropertyReportAppeal appeal = PropertyReportAppeal.create(
                REPORT_ID,
                2001L,
                "운영조치에 이의를 신청하는 상세 사유입니다.",
                ActorContext.member(2001L, ActorRole.USER, "appeal-test")
        );
        ReflectionTestUtils.setField(appeal, "appealId", APPEAL_ID);
        ReflectionTestUtils.setField(appeal, "status", status);
        ReflectionTestUtils.setField(appeal, "version", version);
        when(appealRepository.findByAppealIdAndDeletedAtIsNull(APPEAL_ID))
                .thenReturn(Optional.of(appeal));
        return appeal;
    }

    private void prepareLatestAllowedAction(Long actorMemberId) {
        PropertyReportAction action = PropertyReportAction.create(
                REPORT_ID,
                884700000000000001L,
                ReportActionCode.REQUEST_CORRECTION,
                "운영조치 사유",
                Instant.now(),
                admin(actorMemberId)
        );
        when(actionRepository
                .findFirstByReportIdAndActionCodeInOrderByExecutedAtDescActionIdDesc(
                        REPORT_ID,
                        allowedActionCodes()
                )).thenReturn(Optional.of(action));
    }

    private PropertyReportAppealAdminReviewRequest request(
            AppealStatus status,
            String reason,
            Long version
    ) {
        return new PropertyReportAppealAdminReviewRequest(status, reason, version);
    }

    private List<ReportActionCode> allowedActionCodes() {
        return List.of(
                ReportActionCode.HIDE_PROPERTY,
                ReportActionCode.REQUEST_CORRECTION
        );
    }

    private ActorContext admin(Long memberId) {
        return ActorContext.member(
                memberId,
                ActorRole.CS_ADMIN,
                "appeal-admin-review-test"
        );
    }
}
