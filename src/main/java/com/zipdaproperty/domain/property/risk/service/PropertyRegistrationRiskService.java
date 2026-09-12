package com.zipdaproperty.domain.property.risk.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.model.PreparedPropertyAddress;
import com.zipdaproperty.domain.property.risk.entity.PropertyRiskAssessment;
import com.zipdaproperty.domain.property.risk.model.PropertyRiskAssessmentResult;
import com.zipdaproperty.domain.property.risk.repository.PropertyRegistrationRiskQueryRepository;
import com.zipdaproperty.domain.property.risk.repository.PropertyRiskAssessmentRepository;
import com.zipdaproperty.domain.property.service.PropertyAddressNormalizer;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.FileOwnershipRequiredException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PropertyRegistrationRiskService {

    private final PropertyFileRepository propertyFileRepository;
    private final PropertyRegistrationRiskQueryRepository riskQueryRepository;
    private final PropertyRiskAssessmentRepository riskAssessmentRepository;
    private final PropertyRegistrationRiskPolicy riskPolicy;
    private final PropertyAddressNormalizer propertyAddressNormalizer;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PropertyRiskAssessmentResult evaluateAndRecord(
            Long propertyId,
            PropertyCreateCommand command,
            PreparedPropertyAddress preparedAddress,
            ActorContext actorContext
    ) {
        Set<String> imageChecksums = loadImageChecksums(
                command.fileIds(),
                actorContext
        );

        Set<Long> sameAddressPropertyIds =
                riskQueryRepository.findActivePropertyIdsByAddress(
                        preparedAddress.roadAddress(),
                        preparedAddress.jibunAddress()
                );

        Set<Long> sameAddressPricePublisherPropertyIds =
                riskQueryRepository
                        .findActivePropertyIdsByAddressPricePublisher(
                                preparedAddress.roadAddress(),
                                preparedAddress.jibunAddress(),
                                command,
                                actorContext.memberId()
                        );

        Set<Long> duplicateImagePropertyIds =
                riskQueryRepository
                        .findActivePropertyIdsByImageChecksums(
                                imageChecksums
                        );

        PropertyRiskAssessmentResult result = riskPolicy.evaluate(
                command,
                sameAddressPropertyIds,
                sameAddressPricePublisherPropertyIds,
                duplicateImagePropertyIds
        );

        String normalizedAddressHash =
                propertyAddressNormalizer.createNormalizedAddressHash(
                        preparedAddress.roadAddress(),
                        preparedAddress.jibunAddress()
                );

        PropertyRiskAssessment assessment =
                PropertyRiskAssessment.create(
                        propertyId,
                        actorContext.memberId(),
                        normalizedAddressHash,
                        objectMapper.writeValueAsString(
                                result.matchedRuleCodes()
                        ),
                        result.score(),
                        result.decision(),
                        result.duplicatePropertyId(),
                        Instant.now(),
                        actorContext
                );

        riskAssessmentRepository.save(assessment);
        return result;
    }

    private Set<String> loadImageChecksums(
            List<Long> fileIds,
            ActorContext actorContext
    ) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw invalidRequest(
                    "위험검사에는 하나 이상의 이미지 파일이 필요합니다."
            );
        }

        List<PropertyFile> files = propertyFileRepository
                .findAllByPropertyFileIdInAndDeletedAtIsNull(fileIds);

        if (files.size() != new LinkedHashSet<>(fileIds).size()) {
            throw invalidRequest(
                    "위험검사 대상 이미지 파일을 모두 찾을 수 없습니다."
            );
        }

        LinkedHashSet<String> checksums = new LinkedHashSet<>();
        for (PropertyFile file : files) {
            validateRiskCheckFile(file, actorContext);

            String checksum = file.getChecksum();
            if (checksum == null
                    || !checksum.matches("(?i)[0-9a-f]{64}")) {
                throw invalidRequest(
                        "SHA-256 검증이 완료된 이미지만 등록할 수 있습니다."
                );
            }
            checksums.add(checksum.toLowerCase(Locale.ROOT));
        }
        return checksums;
    }

    private void validateRiskCheckFile(
            PropertyFile file,
            ActorContext actorContext
    ) {
        if (actorContext == null
                || actorContext.memberId() == null
                || !actorContext.memberId().equals(
                        file.getOwnerMemberId()
                )) {
            throw new FileOwnershipRequiredException(
                    "본인이 업로드한 이미지만 위험검사에 사용할 수 있습니다."
            );
        }

        if (file.getFilePurpose() != FilePurpose.PROPERTY_IMAGE
                || !file.isReadyToLink()) {
            throw invalidRequest(
                    "연결 가능한 검증 완료 매물 이미지만 등록할 수 있습니다."
            );
        }
    }

    private BusinessException invalidRequest(String message) {
        return new BusinessException(
                CustomResponseCode.INVALID_REQUEST,
                message
        );
    }
}
