package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.storage.MinioPresignedGetUrlGenerator;
import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyAddress;
import com.zipdaproperty.domain.property.repository.PropertyAddressRepository;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.service.PropertyAdminReviewAccess;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationEvidenceType;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerificationEvidence;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationEvidenceRepository;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyVerificationAdminDetailServiceTest {

  private final PropertyVerificationRepository verificationRepository =
      mock(PropertyVerificationRepository.class);

  private final PropertyRepository propertyRepository =
      mock(PropertyRepository.class);

  private final PropertyAddressRepository propertyAddressRepository =
      mock(PropertyAddressRepository.class);

  private final PropertyVerificationEvidenceRepository evidenceRepository =
      mock(PropertyVerificationEvidenceRepository.class);

  private final PropertyFileRepository fileRepository =
      mock(PropertyFileRepository.class);

  private final MinioPresignedGetUrlGenerator urls =
      mock(MinioPresignedGetUrlGenerator.class);

  private final PropertyAuditEventRecorder audit =
      mock(PropertyAuditEventRecorder.class);

  private final PropertyVerificationAdminDetailService service =
      new PropertyVerificationAdminDetailService(
          verificationRepository,
          propertyRepository,
          propertyAddressRepository,
          evidenceRepository,
          fileRepository,
          urls,
          audit,
          new PropertyAdminReviewAccess()
      );

  private final ActorContext applicant =
      ActorContext.member(
          10L,
          ActorRole.USER,
          "test"
      );

  private final ActorContext admin =
      ActorContext.member(
          20L,
          ActorRole.CS_ADMIN,
          "test"
      );

  @Test
  void linkedActiveVerificationEvidenceGetsUrlAndDetailAudit() {

    Instant submittedAt =
        Instant.ofEpochSecond(42);

    var verification =
        PropertyVerification.submit(
            11L,
            12L,
            PropertyVerificationType.OWNER,
            10L,
            1,
            submittedAt,
            applicant
        );

    Property property =
        mock(Property.class);

    when(property.getPropertyId())
        .thenReturn(12L);

    when(property.getVersion())
        .thenReturn(3L);

    when(property.getTitle())
        .thenReturn("관리자 검수 테스트 매물");

    PropertyAddress address =
        mock(PropertyAddress.class);

    Point exactLocation =
        mock(Point.class);

    when(exactLocation.getX())
        .thenReturn(127.0);

    when(exactLocation.getY())
        .thenReturn(37.5);

    when(address.getExactLocation())
        .thenReturn(exactLocation);

    when(address.getExactRoadAddress())
        .thenReturn("서울시 테스트로 1");

    when(address.getExactJibunAddress())
        .thenReturn("서울시 테스트동 1");

    when(address.getLegalDongCode())
        .thenReturn("1111111111");

    var evidence =
        new PropertyVerificationEvidence(
            13L,
            11L,
            14L,
            PropertyVerificationEvidenceType.REGISTRY_DOCUMENT,
            0,
            applicant
        );

    PropertyFile file =
        PropertyFile.create(
            14L,
            "session",
            FilePurpose.VERIFICATION,
            "proof.pdf",
            3L,
            "secret-key",
            Instant.now().plusSeconds(60),
            applicant
        );

    file.complete(
        "checksum",
        "application/pdf",
        applicant
    );

    file.markLinked(applicant);

    when(verificationRepository
        .findByPropertyVerificationIdAndDeletedAtIsNull(11L))
        .thenReturn(Optional.of(verification));

    when(propertyRepository
        .findByPropertyIdAndDeletedAtIsNull(12L))
        .thenReturn(Optional.of(property));

    when(propertyAddressRepository
        .findByProperty_PropertyIdAndDeletedAtIsNull(12L))
        .thenReturn(Optional.of(address));

    when(evidenceRepository
        .findAllByPropertyVerificationIdAndDeletedAtIsNullOrderBySortOrderAscVerificationEvidenceIdAsc(
            11L
        ))
        .thenReturn(List.of(evidence));

    when(fileRepository
        .findAllByPropertyFileIdInAndDeletedAtIsNull(
            List.of(14L)
        ))
        .thenReturn(List.of(file));

    when(urls.generate("secret-key"))
        .thenReturn("https://example.test/url");

    var response =
        service.find(
            11L,
            "  검수  ",
            admin
        );

    assertThat(response.propertyVersion())
        .isEqualTo(3L);

    assertThat(response.title())
        .isEqualTo("관리자 검수 테스트 매물");

    assertThat(response.address()
        .roadAddress())
        .isEqualTo("서울시 테스트로 1");

    assertThat(response.evidence())
        .hasSize(1);

    assertThat(response.evidence()
        .getFirst()
        .downloadUrl())
        .isEqualTo(
            "https://example.test/url"
        );

    verify(audit)
        .recordAction(
            eq("PROPERTY_VERIFICATION"),
            eq("11"),
            eq(
                PropertyAuditActionCode
                    .PROPERTY_VERIFICATION_DETAIL_VIEWED
            ),
            eq("검수"),
            eq(null),
            any(Instant.class),
            eq(admin)
        );
  }

  @Test
  void missingReasonAndNonAdminCannotReadOrAudit() {

    assertThatThrownBy(
        () -> service.find(
            11L,
            "",
            admin
        )
    ).isInstanceOf(BusinessException.class);

    assertThatThrownBy(
        () -> service.find(
            11L,
            "검수",
            applicant
        )
    ).isInstanceOf(BusinessException.class);

    verify(
        verificationRepository,
        never()
    ).findByPropertyVerificationIdAndDeletedAtIsNull(
        any()
    );

    verify(audit, never())
        .recordAction(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        );
  }
}
