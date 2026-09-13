package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
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
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PropertyReportService {

    private static final int DAILY_REPORT_LIMIT = 5;
    private static final int MAX_EVIDENCE_FILE_COUNT = 5;

    private static final String ACTIVE_REPORT_UNIQUE_CONSTRAINT =
            "uq_property_report_01";

    private static final String ACTIVE_REPORT_GENERATED_COLUMN =
            "active_report_key";

    private static final List<ReportStatus> ACTIVE_REPORT_STATUSES = List.of(
            ReportStatus.RECEIVED,
            ReportStatus.TRIAGED,
            ReportStatus.IN_REVIEW,
            ReportStatus.ACTIONED
    );

    private final PropertyReportRepository propertyReportRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyReportEvidenceRepository evidenceRepository;
    private final PropertyFileRepository propertyFileRepository;
    private final TsidGenerator tsidGenerator;

    @Transactional
    public PropertyReportCreateResponse createReport(
            Long propertyId,
            ReportReasonCode reasonCode,
            String detail,
            List<Long> evidenceFileIds,
            ActorContext actorContext
    ) {
        validateReportActor(actorContext);

        propertyRepository.findByPropertyIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.PROPERTY_NOT_FOUND,
                        "신고할 수 있는 매물이 없습니다."
                ));

        Long reporterMemberId = actorContext.memberId();

        long dailyReportCount = propertyReportRepository
                .countDailyReportsIncludingDeleted(reporterMemberId);
        if (dailyReportCount >= DAILY_REPORT_LIMIT) {
            throw new BusinessException(
                    CustomResponseCode.RATE_LIMITED,
                    "하루 신고 가능 횟수를 초과했습니다."
            );
        }

        boolean hasActiveReport =
                propertyReportRepository
                        .existsByReporterMemberIdAndPropertyIdAndReasonCodeAndStatusInAndDeletedAtIsNull(
                                reporterMemberId,
                                propertyId,
                                reasonCode,
                                ACTIVE_REPORT_STATUSES
                        );

        if (hasActiveReport) {
            throw new BusinessException(
                    CustomResponseCode.DUPLICATE_ACTIVE_REPORT,
                    "동일한 사유의 진행 중 신고가 이미 존재합니다."
            );
        }

        List<PropertyFile> evidenceFiles = validateEvidenceFiles(
                evidenceFileIds,
                actorContext
        );

        PropertyReport propertyReport = PropertyReport.create(
                tsidGenerator.generate(),
                propertyId,
                reporterMemberId,
                reasonCode,
                detail,
                actorContext
        );

        PropertyReport savedReport = saveReport(propertyReport);
        saveEvidence(
                savedReport.getReportId(),
                evidenceFiles,
                actorContext
        );
        evidenceFiles.forEach(file -> file.markLinked(actorContext));

        return new PropertyReportCreateResponse(
                savedReport.getReportId(),
                savedReport.getStatus(),
                savedReport.getVersion()
        );
    }

    private List<PropertyFile> validateEvidenceFiles(
            List<Long> evidenceFileIds,
            ActorContext actorContext
    ) {
        if (evidenceFileIds == null || evidenceFileIds.isEmpty()) {
            return List.of();
        }
        if (evidenceFileIds.size() > MAX_EVIDENCE_FILE_COUNT) {
            throw invalidEvidence("신고 증빙 파일은 최대 5개까지 등록할 수 있습니다.");
        }

        LinkedHashSet<Long> uniqueFileIds = new LinkedHashSet<>();
        for (Long fileId : evidenceFileIds) {
            if (fileId == null) {
                throw invalidEvidence("증빙 파일 ID는 null일 수 없습니다.");
            }
            if (!uniqueFileIds.add(fileId)) {
                throw invalidEvidence("같은 증빙 파일을 중복으로 등록할 수 없습니다.");
            }
        }

        List<Long> lockOrderedFileIds = uniqueFileIds.stream()
                .sorted()
                .toList();
        List<PropertyFile> files = propertyFileRepository
                .findAllForReportEvidenceLink(lockOrderedFileIds);
        if (files.size() != uniqueFileIds.size()) {
            throw invalidEvidence("신고 증빙 파일을 찾을 수 없습니다.");
        }

        for (PropertyFile file : files) {
            validateEvidenceFile(file, actorContext);
        }

        Map<Long, PropertyFile> filesById = files.stream()
                .collect(Collectors.toMap(
                        PropertyFile::getPropertyFileId,
                        Function.identity()
                ));
        return evidenceFileIds.stream()
                .map(filesById::get)
                .toList();
    }

    private void validateEvidenceFile(
            PropertyFile file,
            ActorContext actorContext
    ) {
        if (!file.getOwnerMemberId().equals(actorContext.memberId())) {
            throw new BusinessException(
                    CustomResponseCode.FILE_OWNERSHIP_REQUIRED,
                    "본인이 업로드한 신고 증빙 파일만 연결할 수 있습니다."
            );
        }
        if (file.getFilePurpose() != FilePurpose.REPORT_EVIDENCE) {
            throw invalidEvidence("신고 증빙 용도로 업로드한 파일만 연결할 수 있습니다.");
        }
        if (!file.isReadyToLink()) {
            throw invalidEvidence("VERIFIED 상태의 신고 증빙 파일만 연결할 수 있습니다.");
        }
    }

    private void saveEvidence(
            Long reportId,
            List<PropertyFile> evidenceFiles,
            ActorContext actorContext
    ) {
        if (evidenceFiles.isEmpty()) {
            return;
        }

        List<PropertyReportEvidence> evidence = IntStream
                .range(0, evidenceFiles.size())
                .mapToObj(index -> PropertyReportEvidence.create(
                        reportId,
                        evidenceFiles.get(index).getPropertyFileId(),
                        ReportEvidenceType.SCREENSHOT,
                        index,
                        actorContext
                ))
                .toList();
        evidenceRepository.saveAllAndFlush(evidence);
    }

    private BusinessException invalidEvidence(String message) {
        return new BusinessException(CustomResponseCode.INVALID_REQUEST, message);
    }

    private PropertyReport saveReport(PropertyReport propertyReport) {
        try {
            return propertyReportRepository.saveAndFlush(propertyReport);
        } catch (DataIntegrityViolationException exception) {
            if (isActiveReportUniqueViolation(exception)) {
                throw new BusinessException(
                        CustomResponseCode.DUPLICATE_ACTIVE_REPORT,
                        "동일한 사유의 진행 중 신고가 이미 존재합니다."
                );
            }

            throw exception;
        }
    }

    private boolean isActiveReportUniqueViolation(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && isActiveReportConstraint(constraintViolation.getConstraintName())) {
                return true;
            }

            if (isActiveReportDuplicateMessage(cause.getMessage())) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private boolean isActiveReportConstraint(String constraintName) {
        return constraintName != null
                && ACTIVE_REPORT_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName);
    }

    private boolean isActiveReportDuplicateMessage(String message) {
        if (message == null) {
            return false;
        }

        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        boolean duplicateViolation = normalizedMessage.contains("duplicate entry")
                || normalizedMessage.contains("duplicate key")
                || normalizedMessage.contains("unique constraint")
                || normalizedMessage.contains("unique index");

        return duplicateViolation
                && (normalizedMessage.contains(ACTIVE_REPORT_UNIQUE_CONSTRAINT)
                || normalizedMessage.contains(ACTIVE_REPORT_GENERATED_COLUMN));
    }

    private void validateReportActor(ActorContext actorContext) {
        if (actorContext == null || !actorContext.isMemberRequest()) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "신고 기능을 사용할 수 없는 요청 주체입니다."
            );
        }

        boolean allowedRole = actorContext.role() == ActorRole.USER
                || actorContext.role() == ActorRole.AGENT;

        if (!allowedRole) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "신고 기능을 사용할 수 없는 요청 주체입니다."
            );
        }
    }
}
