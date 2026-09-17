package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.response.PropertyEditDetailResponse;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyPublicationAdminDetailServiceTest {

  private final PropertyEditDetailService edit =
      mock(PropertyEditDetailService.class);

  private final PropertyRepository propertyRepository =
      mock(PropertyRepository.class);

  private final PropertyVerificationRepository verificationRepository =
      mock(PropertyVerificationRepository.class);

  private final PropertyAuditEventRecorder audit =
      mock(PropertyAuditEventRecorder.class);

  private final PropertyPublicationAdminDetailService service =
      new PropertyPublicationAdminDetailService(
          edit,
          propertyRepository,
          verificationRepository,
          audit,
          new PropertyAdminReviewAccess()
      );

  private final ActorContext admin =
      ActorContext.member(
          20L,
          ActorRole.SUPER_ADMIN,
          "test"
      );

  @Test
  void usesEditDetailAndIncludesLatestVerificationWithAudit() {

    Property propertyEntity = mock(Property.class);

    when(propertyEntity.getAuthorMemberId())
        .thenReturn(10L);

    when(propertyRepository
        .findByPropertyIdAndDeletedAtIsNull(12L))
        .thenReturn(Optional.of(propertyEntity));

    PropertyEditDetailResponse property =
        mock(PropertyEditDetailResponse.class);

    ActorContext applicant =
        ActorContext.member(
            10L,
            ActorRole.USER,
            "test"
        );

    PropertyVerification verification =
        PropertyVerification.submit(
            11L,
            12L,
            PropertyVerificationType.OWNER,
            10L,
            1,
            Instant.ofEpochSecond(42),
            applicant
        );

    when(edit.getEditDetail(12L, admin))
        .thenReturn(property);

    when(verificationRepository
        .findTopByPropertyIdAndDeletedAtIsNullOrderBySubmittedAtDescPropertyVerificationIdDesc(
            12L
        ))
        .thenReturn(Optional.of(verification));

    var response =
        service.find(
            12L,
            "  공개 검수  ",
            admin
        );

    assertThat(response.authorMemberId())
        .isEqualTo(10L);

    assertThat(response.property())
        .isSameAs(property);

    assertThat(response.latestVerification()
        .verificationId())
        .isEqualTo(11L);

    assertThat(response.latestVerification()
        .verifiedAt())
        .isNull();

    verify(audit)
        .recordAction(
            eq("PROPERTY_PUBLICATION_REVIEW"),
            eq("12"),
            eq(
                PropertyAuditActionCode
                    .PROPERTY_PUBLICATION_REVIEW_DETAIL_VIEWED
            ),
            eq("공개 검수"),
            eq(null),
            any(Instant.class),
            eq(admin)
        );
  }

  @Test
  void missingReasonRejectsBeforeLoadingSensitiveDetail() {

    assertThatThrownBy(
        () -> service.find(
            12L,
            null,
            admin
        )
    ).isInstanceOf(BusinessException.class);

    verify(propertyRepository, never())
        .findByPropertyIdAndDeletedAtIsNull(any());

    verify(edit, never())
        .getEditDetail(any(), any());
  }
}
