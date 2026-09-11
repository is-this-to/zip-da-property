package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
}
