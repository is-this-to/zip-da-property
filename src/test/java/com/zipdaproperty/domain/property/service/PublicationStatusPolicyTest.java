package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicationStatusPolicyTest {

    private final PublicationStatusPolicy policy =
            new PublicationStatusPolicy();

    @Test
    void validateTransition_allowsDefinedTransitions() {
        assertThatCode(
                () -> policy.validateTransition(
                        PublicationStatus.IN_REVIEW,
                        PublicationStatus.PUBLISHED,
                        TransactionStatus.AVAILABLE
                )
        ).doesNotThrowAnyException();

        assertThatCode(
                () -> policy.validateTransition(
                        PublicationStatus.IN_REVIEW,
                        PublicationStatus.REJECTED,
                        TransactionStatus.AVAILABLE
                )
        ).doesNotThrowAnyException();

        assertThatCode(
                () -> policy.validateTransition(
                        PublicationStatus.REJECTED,
                        PublicationStatus.IN_REVIEW,
                        TransactionStatus.AVAILABLE
                )
        ).doesNotThrowAnyException();

        assertThatCode(
                () -> policy.validateTransition(
                        PublicationStatus.PUBLISHED,
                        PublicationStatus.HIDDEN,
                        TransactionStatus.RESERVED
                )
        ).doesNotThrowAnyException();

        assertThatCode(
                () -> policy.validateTransition(
                        PublicationStatus.HIDDEN,
                        PublicationStatus.PUBLISHED,
                        TransactionStatus.AVAILABLE
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void validateTransition_rejectsUndefinedTransition() {
        assertThatThrownBy(
                () -> policy.validateTransition(
                        PublicationStatus.PUBLISHED,
                        PublicationStatus.REJECTED,
                        TransactionStatus.AVAILABLE
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(
                        exception.getCustomResponseCode()
                ).isEqualTo(
                        CustomResponseCode.INVALID_STATUS_TRANSITION
                )
        );
    }

    @Test
    void validateTransition_rejectsRepublishWhenTransactionIsNotAvailable() {
        assertThatThrownBy(
                () -> policy.validateTransition(
                        PublicationStatus.HIDDEN,
                        PublicationStatus.PUBLISHED,
                        TransactionStatus.RESERVED
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(
                        exception.getCustomResponseCode()
                ).isEqualTo(
                        CustomResponseCode.INVALID_STATUS_TRANSITION
                )
        );
    }

    @Test
    void published_requiresMatchingPublisherVerificationForReviewAndRepublish() {
        for (PublicationStatus current : new PublicationStatus[]{PublicationStatus.IN_REVIEW, PublicationStatus.HIDDEN}) {
            assertThatCode(() -> policy.validatePublicationVerification(
                    current, PublicationStatus.PUBLISHED, PublisherType.DIRECT_OWNER,
                    VerificationStatus.OWNER_VERIFIED)).doesNotThrowAnyException();
            assertThatCode(() -> policy.validatePublicationVerification(
                    current, PublicationStatus.PUBLISHED, PublisherType.DIRECT_TENANT,
                    VerificationStatus.TENANT_VERIFIED)).doesNotThrowAnyException();
            assertThatCode(() -> policy.validatePublicationVerification(
                    current, PublicationStatus.PUBLISHED, PublisherType.AGENT_BROKERAGE,
                    VerificationStatus.AGENT_VERIFIED)).doesNotThrowAnyException();
            for (VerificationStatus invalid : new VerificationStatus[]{
                    VerificationStatus.UNVERIFIED, VerificationStatus.IN_REVIEW,
                    VerificationStatus.REJECTED, VerificationStatus.EXPIRED,
                    VerificationStatus.TENANT_VERIFIED}) {
                assertThatThrownBy(() -> policy.validatePublicationVerification(
                        current, PublicationStatus.PUBLISHED, PublisherType.DIRECT_OWNER, invalid))
                        .isInstanceOf(BusinessException.class);
            }
        }
        assertThatCode(() -> policy.validatePublicationVerification(
                PublicationStatus.IN_REVIEW, PublicationStatus.REJECTED,
                PublisherType.DIRECT_OWNER, VerificationStatus.UNVERIFIED)).doesNotThrowAnyException();
    }
}
