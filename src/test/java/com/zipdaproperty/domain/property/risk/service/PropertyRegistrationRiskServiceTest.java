package com.zipdaproperty.domain.property.risk.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.model.PreparedPropertyAddress;
import com.zipdaproperty.domain.property.risk.constant.PropertyRiskDecision;
import com.zipdaproperty.domain.property.risk.constant.PropertyRiskRuleCode;
import com.zipdaproperty.domain.property.risk.entity.PropertyRiskAssessment;
import com.zipdaproperty.domain.property.risk.model.PropertyRiskAssessmentResult;
import com.zipdaproperty.domain.property.risk.repository.PropertyRegistrationRiskQueryRepository;
import com.zipdaproperty.domain.property.risk.repository.PropertyRiskAssessmentRepository;
import com.zipdaproperty.domain.property.service.PropertyAddressNormalizer;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.FileOwnershipRequiredException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyRegistrationRiskServiceTest {

    private static final Long PROPERTY_ID = 884700000000000001L;
    private static final Long MEMBER_ID = 1001L;
    private static final String CHECKSUM = "a".repeat(64);

    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);
    private final PropertyRegistrationRiskQueryRepository riskQueryRepository =
            mock(PropertyRegistrationRiskQueryRepository.class);
    private final PropertyRiskAssessmentRepository riskAssessmentRepository =
            mock(PropertyRiskAssessmentRepository.class);
    private final PropertyRegistrationRiskPolicy riskPolicy =
            mock(PropertyRegistrationRiskPolicy.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);

    private PropertyRegistrationRiskService service;

    private final ActorContext actorContext = ActorContext.member(
            MEMBER_ID,
            ActorRole.USER,
            "property-risk-test"
    );

    @BeforeEach
    void setUp() {
        service = new PropertyRegistrationRiskService(
                propertyFileRepository,
                riskQueryRepository,
                riskAssessmentRepository,
                riskPolicy,
                new PropertyAddressNormalizer(),
                objectMapper
        );
    }

    @Test
    void evaluateAndRecord_validSignals_savesRuleCodesAndScore() {
        PropertyCreateCommand command = mock(PropertyCreateCommand.class);
        when(command.fileIds()).thenReturn(List.of(101L));

        PropertyFile file = mock(PropertyFile.class);
        when(file.getChecksum()).thenReturn(CHECKSUM);
        when(file.getOwnerMemberId()).thenReturn(MEMBER_ID);
        when(file.getFilePurpose()).thenReturn(FilePurpose.PROPERTY_IMAGE);
        when(file.isReadyToLink()).thenReturn(true);
        when(propertyFileRepository
                .findAllByPropertyFileIdInAndDeletedAtIsNull(List.of(101L)))
                .thenReturn(List.of(file));

        PreparedPropertyAddress address = preparedAddress();
        when(riskQueryRepository.findActivePropertyIdsByAddress(
                address.roadAddress(),
                address.jibunAddress()
        )).thenReturn(Set.of(200L));
        when(riskQueryRepository
                .findActivePropertyIdsByAddressPricePublisher(
                        address.roadAddress(),
                        address.jibunAddress(),
                        command,
                        MEMBER_ID
                )).thenReturn(Set.of(200L));
        when(riskQueryRepository.findActivePropertyIdsByImageChecksums(
                Set.of(CHECKSUM)
        )).thenReturn(Set.of());

        PropertyRiskAssessmentResult expected =
                new PropertyRiskAssessmentResult(
                        BigDecimal.valueOf(70),
                        PropertyRiskDecision.IN_REVIEW,
                        List.of(
                                PropertyRiskRuleCode
                                        .SAME_ADDRESS_PRICE_PUBLISHER
                        ),
                        200L
                );
        when(riskPolicy.evaluate(
                command,
                Set.of(200L),
                Set.of(200L),
                Set.of()
        )).thenReturn(expected);
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("[\"SAME_ADDRESS_PRICE_PUBLISHER\"]");

        PropertyRiskAssessmentResult actual = service.evaluateAndRecord(
                PROPERTY_ID,
                command,
                address,
                actorContext
        );

        assertThat(actual).isSameAs(expected);

        ArgumentCaptor<PropertyRiskAssessment> captor =
                ArgumentCaptor.forClass(PropertyRiskAssessment.class);
        verify(riskAssessmentRepository).save(captor.capture());

        PropertyRiskAssessment saved = captor.getValue();
        assertThat(saved.getPropertyId()).isEqualTo(PROPERTY_ID);
        assertThat(saved.getPublisherMemberId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getNormalizedAddressHash())
                .matches("[0-9a-f]{64}");
        assertThat(saved.getRiskScore()).isEqualByComparingTo("70");
        assertThat(saved.getDecision())
                .isEqualTo(PropertyRiskDecision.IN_REVIEW);
        assertThat(saved.getDuplicatePropertyId()).isEqualTo(200L);
        assertThat(saved.getMatchedRuleCodesJson())
                .isEqualTo("[\"SAME_ADDRESS_PRICE_PUBLISHER\"]");
    }

    @Test
    void evaluateAndRecord_missingChecksum_rejectsBeforeQueries() {
        PropertyCreateCommand command = mock(PropertyCreateCommand.class);
        when(command.fileIds()).thenReturn(List.of(101L));

        PropertyFile file = mock(PropertyFile.class);
        when(file.getChecksum()).thenReturn(null);
        when(file.getOwnerMemberId()).thenReturn(MEMBER_ID);
        when(file.getFilePurpose()).thenReturn(FilePurpose.PROPERTY_IMAGE);
        when(file.isReadyToLink()).thenReturn(true);
        when(propertyFileRepository
                .findAllByPropertyFileIdInAndDeletedAtIsNull(List.of(101L)))
                .thenReturn(List.of(file));

        assertThatThrownBy(() -> service.evaluateAndRecord(
                PROPERTY_ID,
                command,
                preparedAddress(),
                actorContext
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("customResponseCode")
                .isEqualTo(CustomResponseCode.INVALID_REQUEST);

        verifyNoInteractions(
                riskQueryRepository,
                riskPolicy,
                riskAssessmentRepository
        );
    }

    @Test
    void evaluateAndRecord_foreignImage_rejectsBeforeQueries() {
        PropertyCreateCommand command = mock(PropertyCreateCommand.class);
        when(command.fileIds()).thenReturn(List.of(101L));

        PropertyFile file = mock(PropertyFile.class);
        when(file.getOwnerMemberId()).thenReturn(9999L);
        when(propertyFileRepository
                .findAllByPropertyFileIdInAndDeletedAtIsNull(List.of(101L)))
                .thenReturn(List.of(file));

        assertThatThrownBy(() -> service.evaluateAndRecord(
                PROPERTY_ID,
                command,
                preparedAddress(),
                actorContext
        )).isInstanceOf(FileOwnershipRequiredException.class);

        verifyNoInteractions(
                riskQueryRepository,
                riskPolicy,
                riskAssessmentRepository
        );
    }

    private PreparedPropertyAddress preparedAddress() {
        GeometryFactory geometryFactory = new GeometryFactory(
                new PrecisionModel(),
                4326
        );
        Point exact = geometryFactory.createPoint(
                new Coordinate(128.625123, 35.859321)
        );
        Point publicPoint = geometryFactory.createPoint(
                new Coordinate(128.625500, 35.859500)
        );

        return new PreparedPropertyAddress(
                53390L,
                "2726010100",
                "대구 수성구 달구벌대로 2450",
                "대구광역시 수성구 범어동 123",
                exact,
                "대구광역시 수성구 범어동",
                publicPoint,
                Instant.parse("2026-09-12T00:00:00Z")
        );
    }
}
