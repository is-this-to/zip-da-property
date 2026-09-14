package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.entity.PropertyReportEvidence;
import com.zipdaproperty.domain.report.repository.PropertyReportEvidenceRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportRepository;
import com.zipdaproperty.domain.report.response.PropertyReportCreateResponse;
import com.zipdaproperty.domain.report.type.ReportEvidenceType;
import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.id.TsidGenerator;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyReportServiceTest {

    private static final Long PROPERTY_ID = 884700000000000001L;
    private static final Long REPORT_ID = 884700000000000002L;
    private static final Long FIRST_FILE_ID = 884700000000000003L;
    private static final Long SECOND_FILE_ID = 884700000000000004L;
    private static final Long REPORTER_MEMBER_ID = 1001L;
    private static final Long INITIAL_VERSION = 0L;
    private static final String DETAIL = "허위 매물 정보가 포함되어 있어 신고합니다.";

    private final PropertyReportRepository propertyReportRepository =
            mock(PropertyReportRepository.class);
    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);
    private final PropertyReportEvidenceRepository evidenceRepository =
            mock(PropertyReportEvidenceRepository.class);
    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);
    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);

    private PropertyReportService service;

    @BeforeEach
    void setUp() {
        service = new PropertyReportService(
                propertyReportRepository,
                propertyRepository,
                evidenceRepository,
                propertyFileRepository,
                tsidGenerator
        );

        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID))
                .thenReturn(Optional.of(mock(Property.class)));
        when(propertyReportRepository
                .countDailyReportsIncludingDeleted(REPORTER_MEMBER_ID))
                .thenReturn(0L);
        when(propertyReportRepository
                .existsByReporterMemberIdAndPropertyIdAndReasonCodeAndStatusInAndDeletedAtIsNull(
                        anyLong(),
                        anyLong(),
                        any(ReportReasonCode.class),
                        any()
                )).thenReturn(false);
        when(tsidGenerator.generate()).thenReturn(REPORT_ID);
        when(propertyReportRepository.saveAndFlush(any(PropertyReport.class)))
                .thenAnswer(invocation -> {
                    PropertyReport report = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            report,
                            "version",
                            INITIAL_VERSION
                    );
                    return report;
                });
        when(evidenceRepository.saveAllAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createReport_validRequest_savesReportAndReturnsCreatedReport() {
        ActorContext actorContext = actor(ActorRole.USER);

        PropertyReportCreateResponse response = service.createReport(
                PROPERTY_ID,
                ReportReasonCode.FALSE_INFO,
                DETAIL,
                null,
                actorContext
        );

        ArgumentCaptor<PropertyReport> reportCaptor =
                ArgumentCaptor.forClass(PropertyReport.class);
        verify(propertyReportRepository).saveAndFlush(reportCaptor.capture());

        PropertyReport savedReport = reportCaptor.getValue();
        assertThat(savedReport.getReportId()).isEqualTo(REPORT_ID);
        assertThat(savedReport.getPropertyId()).isEqualTo(PROPERTY_ID);
        assertThat(savedReport.getReporterMemberId())
                .isEqualTo(REPORTER_MEMBER_ID);
        assertThat(savedReport.getReasonCode())
                .isEqualTo(ReportReasonCode.FALSE_INFO);
        assertThat(savedReport.getDetail()).isEqualTo(DETAIL);
        assertThat(savedReport.getStatus()).isEqualTo(ReportStatus.RECEIVED);
        assertThat(savedReport.getCreatedByMemberId())
                .isEqualTo(REPORTER_MEMBER_ID);
        assertThat(savedReport.getCreatedByRole()).isEqualTo(ActorRole.USER);

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        assertThat(response.status()).isEqualTo(ReportStatus.RECEIVED);
        assertThat(response.version()).isEqualTo(INITIAL_VERSION);
        verify(propertyReportRepository)
                .countDailyReportsIncludingDeleted(REPORTER_MEMBER_ID);
    }

    @Test
    void createReport_propertyDoesNotExist_throwsPropertyNotFound() {
        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID))
                .thenReturn(Optional.empty());

        assertBusinessException(
                () -> service.createReport(
                        PROPERTY_ID,
                        ReportReasonCode.DUPLICATE,
                        DETAIL,
                        null,
                        actor(ActorRole.USER)
                ),
                CustomResponseCode.PROPERTY_NOT_FOUND
        );

        verifyNoInteractions(propertyReportRepository, tsidGenerator);
    }

    @Test
    void createReport_sameActiveReportExists_throwsDuplicateActiveReport() {
        when(propertyReportRepository
                .existsByReporterMemberIdAndPropertyIdAndReasonCodeAndStatusInAndDeletedAtIsNull(
                        anyLong(),
                        anyLong(),
                        any(ReportReasonCode.class),
                        any()
                )).thenReturn(true);

        assertBusinessException(
                () -> service.createReport(
                        PROPERTY_ID,
                        ReportReasonCode.UNAVAILABLE,
                        DETAIL,
                        null,
                        actor(ActorRole.USER)
                ),
                CustomResponseCode.DUPLICATE_ACTIVE_REPORT
        );

        verify(propertyReportRepository, never())
                .saveAndFlush(any(PropertyReport.class));
        verifyNoInteractions(tsidGenerator);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 2L, 3L, 4L})
    void createReport_fewerThanFiveSuccessfulReports_allowsNextReport(
            long existingDailyCount
    ) {
        when(propertyReportRepository
                .countDailyReportsIncludingDeleted(REPORTER_MEMBER_ID))
                .thenReturn(existingDailyCount);

        PropertyReportCreateResponse response = service.createReport(
                PROPERTY_ID,
                ReportReasonCode.PRICE_MISMATCH,
                DETAIL,
                null,
                actor(ActorRole.USER)
        );

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        verify(propertyReportRepository)
                .saveAndFlush(any(PropertyReport.class));
    }

    @Test
    void createReport_fiveSuccessfulReportsAlreadyExist_throwsRateLimited() {
        when(propertyReportRepository
                .countDailyReportsIncludingDeleted(REPORTER_MEMBER_ID))
                .thenReturn(5L);

        assertBusinessException(
                () -> service.createReport(
                        PROPERTY_ID,
                        ReportReasonCode.OTHER,
                        DETAIL,
                        null,
                        actor(ActorRole.USER)
                ),
                CustomResponseCode.RATE_LIMITED
        );

        verify(propertyReportRepository, never())
                .existsByReporterMemberIdAndPropertyIdAndReasonCodeAndStatusInAndDeletedAtIsNull(
                        anyLong(),
                        anyLong(),
                        any(ReportReasonCode.class),
                        any()
                );
        verify(propertyReportRepository, never())
                .saveAndFlush(any(PropertyReport.class));
        verifyNoInteractions(tsidGenerator);
    }

    @Test
    void createReport_activeDuplicateCheck_excludesTerminalStatuses() {
        service.createReport(
                PROPERTY_ID,
                ReportReasonCode.FALSE_INFO,
                DETAIL,
                null,
                actor(ActorRole.USER)
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<ReportStatus>> statusesCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(propertyReportRepository)
                .existsByReporterMemberIdAndPropertyIdAndReasonCodeAndStatusInAndDeletedAtIsNull(
                        eq(REPORTER_MEMBER_ID),
                        eq(PROPERTY_ID),
                        eq(ReportReasonCode.FALSE_INFO),
                        statusesCaptor.capture()
                );

        assertThat(statusesCaptor.getValue()).containsExactly(
                ReportStatus.RECEIVED,
                ReportStatus.TRIAGED,
                ReportStatus.IN_REVIEW,
                ReportStatus.ACTIONED
        );
        assertThat(statusesCaptor.getValue()).doesNotContain(
                ReportStatus.REJECTED,
                ReportStatus.CLOSED
        );
    }

    @ParameterizedTest
    @EnumSource(value = ActorRole.class, names = {"USER", "AGENT"})
    void createReport_userAndAgentRoles_areAllowed(ActorRole actorRole) {
        PropertyReportCreateResponse response = service.createReport(
                PROPERTY_ID,
                ReportReasonCode.OTHER,
                DETAIL,
                null,
                actor(actorRole)
        );

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        verify(propertyReportRepository)
                .saveAndFlush(any(PropertyReport.class));
    }

    @Test
    void createReport_uniqueConstraintViolation_isConvertedToDuplicateActiveReport() {
        DataIntegrityViolationException uniqueViolation =
                new DataIntegrityViolationException(
                        "Duplicate entry for unique constraint uq_property_report_01"
                );
        when(propertyReportRepository.saveAndFlush(any(PropertyReport.class)))
                .thenThrow(uniqueViolation);

        assertBusinessException(
                () -> service.createReport(
                        PROPERTY_ID,
                        ReportReasonCode.FALSE_INFO,
                        DETAIL,
                        null,
                        actor(ActorRole.USER)
                ),
                CustomResponseCode.DUPLICATE_ACTIVE_REPORT
        );
    }

    @Test
    void createReport_unrelatedDataIntegrityViolation_isRethrown() {
        DataIntegrityViolationException unrelatedViolation =
                new DataIntegrityViolationException("not null constraint violation");
        when(propertyReportRepository.saveAndFlush(any(PropertyReport.class)))
                .thenThrow(unrelatedViolation);

        assertThatThrownBy(() -> service.createReport(
                PROPERTY_ID,
                ReportReasonCode.FALSE_INFO,
                DETAIL,
                null,
                actor(ActorRole.USER)
        )).isSameAs(unrelatedViolation);
    }

    @Test
    void createReport_emptyEvidence_doesNotLoadOrSaveEvidence() {
        service.createReport(
                PROPERTY_ID,
                ReportReasonCode.FALSE_INFO,
                DETAIL,
                List.of(),
                actor(ActorRole.USER)
        );

        verifyNoInteractions(propertyFileRepository, evidenceRepository);
    }

    @Test
    void createReport_verifiedEvidence_savesRequestOrderAndMarksFilesLinked() {
        PropertyFile firstFile = verifiedEvidenceFile(FIRST_FILE_ID, REPORTER_MEMBER_ID);
        PropertyFile secondFile = verifiedEvidenceFile(SECOND_FILE_ID, REPORTER_MEMBER_ID);
        when(propertyFileRepository.findAllForReportEvidenceLink(
                List.of(FIRST_FILE_ID, SECOND_FILE_ID)
        )).thenReturn(List.of(firstFile, secondFile));

        service.createReport(
                PROPERTY_ID,
                ReportReasonCode.FALSE_INFO,
                DETAIL,
                List.of(SECOND_FILE_ID, FIRST_FILE_ID),
                actor(ActorRole.USER)
        );

        verify(propertyFileRepository).findAllForReportEvidenceLink(
                List.of(FIRST_FILE_ID, SECOND_FILE_ID)
        );
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PropertyReportEvidence>> evidenceCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(evidenceRepository).saveAllAndFlush(evidenceCaptor.capture());
        assertThat(evidenceCaptor.getValue())
                .extracting(PropertyReportEvidence::getPropertyFileId)
                .containsExactly(SECOND_FILE_ID, FIRST_FILE_ID);
        assertThat(evidenceCaptor.getValue())
                .extracting(PropertyReportEvidence::getSortOrder)
                .containsExactly(0, 1);
        assertThat(evidenceCaptor.getValue())
                .allSatisfy(evidence -> {
                    assertThat(evidence.getReportId()).isEqualTo(REPORT_ID);
                    assertThat(evidence.getEvidenceType())
                            .isEqualTo(ReportEvidenceType.SCREENSHOT);
                });
        assertThat(firstFile.getUploadStatus()).isEqualTo(UploadStatus.LINKED);
        assertThat(secondFile.getUploadStatus()).isEqualTo(UploadStatus.LINKED);
    }

    @Test
    void createReport_fiveVerifiedEvidenceFiles_savesAndLinksAllFiles() {
        List<Long> fileIds = List.of(
                884700000000000003L,
                884700000000000004L,
                884700000000000005L,
                884700000000000006L,
                884700000000000007L
        );
        List<PropertyFile> files = fileIds.stream()
                .map(fileId -> verifiedEvidenceFile(fileId, REPORTER_MEMBER_ID))
                .toList();
        when(propertyFileRepository.findAllForReportEvidenceLink(fileIds))
                .thenReturn(files);

        service.createReport(
                PROPERTY_ID,
                ReportReasonCode.FALSE_INFO,
                DETAIL,
                fileIds,
                actor(ActorRole.USER)
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PropertyReportEvidence>> evidenceCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(evidenceRepository).saveAllAndFlush(evidenceCaptor.capture());
        assertThat(evidenceCaptor.getValue()).hasSize(5);
        assertThat(files)
                .allSatisfy(file -> assertThat(file.getUploadStatus())
                        .isEqualTo(UploadStatus.LINKED));
    }

    @Test
    void createReport_sixEvidenceFiles_throwsInvalidRequestBeforeReportSave() {
        assertBusinessException(
                () -> service.createReport(
                        PROPERTY_ID,
                        ReportReasonCode.FALSE_INFO,
                        DETAIL,
                        List.of(1L, 2L, 3L, 4L, 5L, 6L),
                        actor(ActorRole.USER)
                ),
                CustomResponseCode.INVALID_REQUEST
        );

        verifyNoInteractions(propertyFileRepository, evidenceRepository);
        verify(propertyReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void createReport_nullEvidenceFileId_throwsInvalidRequestBeforeReportSave() {
        List<Long> evidenceFileIds = new java.util.ArrayList<>();
        evidenceFileIds.add(FIRST_FILE_ID);
        evidenceFileIds.add(null);

        assertBusinessException(
                () -> service.createReport(
                        PROPERTY_ID,
                        ReportReasonCode.FALSE_INFO,
                        DETAIL,
                        evidenceFileIds,
                        actor(ActorRole.USER)
                ),
                CustomResponseCode.INVALID_REQUEST
        );

        verifyNoInteractions(propertyFileRepository, evidenceRepository);
        verify(propertyReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void createReport_duplicateEvidenceFileId_throwsInvalidRequestBeforeReportSave() {
        assertBusinessException(
                () -> service.createReport(
                        PROPERTY_ID,
                        ReportReasonCode.FALSE_INFO,
                        DETAIL,
                        List.of(FIRST_FILE_ID, FIRST_FILE_ID),
                        actor(ActorRole.USER)
                ),
                CustomResponseCode.INVALID_REQUEST
        );

        verifyNoInteractions(propertyFileRepository, evidenceRepository);
        verify(propertyReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void createReport_missingOrDeletedEvidenceFile_throwsInvalidRequest() {
        when(propertyFileRepository.findAllForReportEvidenceLink(
                List.of(FIRST_FILE_ID)
        )).thenReturn(List.of());

        assertBusinessException(
                () -> createReportWithEvidence(FIRST_FILE_ID),
                CustomResponseCode.INVALID_REQUEST
        );

        verify(propertyReportRepository, never()).saveAndFlush(any());
        verifyNoInteractions(evidenceRepository);
    }

    @Test
    void createReport_otherMembersEvidenceFile_throwsOwnershipRequired() {
        PropertyFile file = verifiedEvidenceFile(FIRST_FILE_ID, 2002L);
        stubEvidenceFiles(file);

        assertBusinessException(
                () -> createReportWithEvidence(FIRST_FILE_ID),
                CustomResponseCode.FILE_OWNERSHIP_REQUIRED
        );

        assertThat(file.getUploadStatus()).isEqualTo(UploadStatus.VERIFIED);
        verify(propertyReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void createReport_wrongPurposeEvidenceFile_throwsInvalidRequest() {
        PropertyFile file = verifiedFile(
                FIRST_FILE_ID,
                REPORTER_MEMBER_ID,
                FilePurpose.PROPERTY_IMAGE
        );
        stubEvidenceFiles(file);

        assertBusinessException(
                () -> createReportWithEvidence(FIRST_FILE_ID),
                CustomResponseCode.INVALID_REQUEST
        );

        assertThat(file.getUploadStatus()).isEqualTo(UploadStatus.VERIFIED);
        verify(propertyReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void createReport_createdEvidenceFile_throwsInvalidRequest() {
        PropertyFile file = evidenceFile(FIRST_FILE_ID, REPORTER_MEMBER_ID);
        stubEvidenceFiles(file);

        assertBusinessException(
                () -> createReportWithEvidence(FIRST_FILE_ID),
                CustomResponseCode.INVALID_REQUEST
        );

        assertThat(file.getUploadStatus()).isEqualTo(UploadStatus.CREATED);
        verify(propertyReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void createReport_linkedEvidenceFile_throwsInvalidRequest() {
        PropertyFile file = verifiedEvidenceFile(FIRST_FILE_ID, REPORTER_MEMBER_ID);
        file.markLinked(actor(ActorRole.USER));
        stubEvidenceFiles(file);

        assertBusinessException(
                () -> createReportWithEvidence(FIRST_FILE_ID),
                CustomResponseCode.INVALID_REQUEST
        );

        verify(propertyReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void createReport_evidenceSaveFailure_doesNotMarkFileLinked() {
        PropertyFile file = verifiedEvidenceFile(FIRST_FILE_ID, REPORTER_MEMBER_ID);
        stubEvidenceFiles(file);
        DataIntegrityViolationException failure =
                new DataIntegrityViolationException("evidence save failed");
        when(evidenceRepository.saveAllAndFlush(any())).thenThrow(failure);

        assertThatThrownBy(() -> createReportWithEvidence(FIRST_FILE_ID))
                .isSameAs(failure);

        assertThat(file.getUploadStatus()).isEqualTo(UploadStatus.VERIFIED);
    }

    private void createReportWithEvidence(Long fileId) {
        service.createReport(
                PROPERTY_ID,
                ReportReasonCode.FALSE_INFO,
                DETAIL,
                List.of(fileId),
                actor(ActorRole.USER)
        );
    }

    private void stubEvidenceFiles(PropertyFile... files) {
        when(propertyFileRepository.findAllForReportEvidenceLink(
                List.of(FIRST_FILE_ID)
        )).thenReturn(List.of(files));
    }

    private PropertyFile verifiedEvidenceFile(Long fileId, Long ownerMemberId) {
        return verifiedFile(fileId, ownerMemberId, FilePurpose.REPORT_EVIDENCE);
    }

    private PropertyFile verifiedFile(
            Long fileId,
            Long ownerMemberId,
            FilePurpose filePurpose
    ) {
        PropertyFile file = evidenceFile(fileId, ownerMemberId, filePurpose);
        file.complete(
                "a".repeat(64),
                "image/jpeg",
                ActorContext.member(ownerMemberId, ActorRole.USER, "report-evidence-test")
        );
        return file;
    }

    private PropertyFile evidenceFile(Long fileId, Long ownerMemberId) {
        return evidenceFile(fileId, ownerMemberId, FilePurpose.REPORT_EVIDENCE);
    }

    private PropertyFile evidenceFile(
            Long fileId,
            Long ownerMemberId,
            FilePurpose filePurpose
    ) {
        return PropertyFile.create(
                fileId,
                "report-evidence-session",
                filePurpose,
                "evidence.jpg",
                1024L,
                "report/evidence/" + fileId,
                Instant.parse("2026-09-15T00:00:00Z"),
                ActorContext.member(ownerMemberId, ActorRole.USER, "report-evidence-test")
        );
    }

    private ActorContext actor(ActorRole role) {
        return ActorContext.member(
                REPORTER_MEMBER_ID,
                role,
                "property-report-service-test"
        );
    }

    private void assertBusinessException(
            ThrowingCallable callable,
            CustomResponseCode expectedCode
    ) {
        assertThatThrownBy(callable::call)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(expectedCode)
                );
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
