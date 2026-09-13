package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyVerificationPolicyTest {

    private static final Long TENANT_MEMBER_ID = 1001L;

    private final PropertyVerificationPolicy policy =
            new PropertyVerificationPolicy();

    @Test
    void validateSubmission_directTenantUser_returnsTenantType() {
        Property property = mock(Property.class);
        ActorContext tenant = ActorContext.member(
                TENANT_MEMBER_ID,
                ActorRole.USER,
                "tenant-verification-policy-test"
        );

        when(property.getAuthorMemberId())
                .thenReturn(TENANT_MEMBER_ID);
        when(property.getPublisherType())
                .thenReturn(PublisherType.DIRECT_TENANT);
        when(property.getVerificationStatus())
                .thenReturn(VerificationStatus.UNVERIFIED);

        PropertyVerificationType type =
                policy.validateSubmission(
                        property,
                        tenant
                );

        assertThat(type)
                .isEqualTo(PropertyVerificationType.TENANT);
    }

    @Test
    void validateRenewalWindow_withinSevenDays_allowsRenewal() {
        Instant currentTime = Instant.parse("2026-09-11T09:00:00Z");
        PropertyVerification verification = approvedVerification(
                currentTime.plusSeconds(6L * 24L * 60L * 60L)
        );

        policy.validateRenewalWindow(
                verification,
                PropertyVerificationType.OWNER,
                currentTime
        );
    }

    @Test
    void validateRenewalWindow_moreThanSevenDaysRemaining_rejectsRenewal() {
        Instant currentTime = Instant.parse("2026-09-11T09:00:00Z");
        PropertyVerification verification = approvedVerification(
                currentTime.plusSeconds(8L * 24L * 60L * 60L)
        );

        assertThatThrownBy(() -> policy.validateRenewalWindow(
                verification,
                PropertyVerificationType.OWNER,
                currentTime
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(
                                CustomResponseCode
                                        .PROPERTY_VERIFICATION_ALREADY_IN_PROGRESS
                        )
        );
    }

    private PropertyVerification approvedVerification(Instant expiresAt) {
        ActorContext owner = ActorContext.member(
                TENANT_MEMBER_ID,
                ActorRole.USER,
                "renewal-policy-origin"
        );
        PropertyVerification verification = PropertyVerification.submit(
                2001L,
                3001L,
                PropertyVerificationType.OWNER,
                TENANT_MEMBER_ID,
                1,
                expiresAt.minusSeconds(30L * 24L * 60L * 60L),
                owner
        );
        verification.approve(
                "승인",
                expiresAt.minusSeconds(30L * 24L * 60L * 60L),
                expiresAt,
                ActorContext.member(
                        9001L,
                        ActorRole.CS_ADMIN,
                        "renewal-policy-review"
                )
        );
        return verification;
    }
}
