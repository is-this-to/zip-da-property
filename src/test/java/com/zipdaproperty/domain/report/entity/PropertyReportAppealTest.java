package com.zipdaproperty.domain.report.entity;

import com.zipdaproperty.domain.report.type.AppealStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyReportAppealTest {

    @Test
    void reviewReason_usesErdLengthLimit() throws Exception {
        Field field = PropertyReportAppeal.class
                .getDeclaredField("reviewReason");

        assertThat(field.getAnnotation(Column.class).length())
                .isEqualTo(1000);
    }

    @Test
    void table_enforcesOneAppealPerReport() {
        Table table = PropertyReportAppeal.class.getAnnotation(Table.class);

        assertThat(table.uniqueConstraints())
                .anySatisfy(constraint -> {
                    assertThat(constraint.name())
                            .isEqualTo("uq_property_report_appeal_report");
                    assertThat(constraint.columnNames())
                            .containsExactly("report_id");
                });
    }

    @Test
    void startReview_recordsReviewerWithoutCompletionFields() {
        PropertyReportAppeal appeal = appeal();
        ActorContext reviewer = admin(3002L);

        appeal.startReview(reviewer);

        assertThat(appeal.getStatus()).isEqualTo(AppealStatus.IN_REVIEW);
        assertThat(appeal.getReviewerMemberId()).isEqualTo(3002L);
        assertThat(appeal.getReviewReason()).isNull();
        assertThat(appeal.getReviewedAt()).isNull();
    }

    @Test
    void completeReview_recordsReasonReviewerAndReviewedAt() {
        PropertyReportAppeal appeal = appeal();
        appeal.startReview(admin(3002L));
        Instant reviewedAt = Instant.parse("2026-09-14T12:00:00Z");

        appeal.completeReview(
                AppealStatus.ACCEPTED,
                "이의신청 승인 사유",
                reviewedAt,
                admin(3003L)
        );

        assertThat(appeal.getStatus()).isEqualTo(AppealStatus.ACCEPTED);
        assertThat(appeal.getReviewerMemberId()).isEqualTo(3003L);
        assertThat(appeal.getReviewReason()).isEqualTo("이의신청 승인 사유");
        assertThat(appeal.getReviewedAt()).isEqualTo(reviewedAt);
    }

    private PropertyReportAppeal appeal() {
        return PropertyReportAppeal.create(
                884685586571263701L,
                2001L,
                "운영조치에 이의를 신청하는 상세 사유입니다.",
                ActorContext.member(2001L, ActorRole.USER, "appeal-test")
        );
    }

    private ActorContext admin(Long memberId) {
        return ActorContext.member(
                memberId,
                ActorRole.CS_ADMIN,
                "appeal-review-test"
        );
    }
}
