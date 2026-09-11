package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.command.PropertyAddressCommand;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyPublisherSnapshot;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.model.PreparedPropertyAddress;
import com.zipdaproperty.domain.property.repository.PropertyPublisherSnapshotRepository;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.repository.PropertyStatusHistoryRepository;
import com.zipdaproperty.domain.property.response.PropertyCreateResponse;
import com.zipdaproperty.domain.region.entity.Region;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.id.TsidGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyCreateServiceTest {

    private static final Long PROPERTY_ID =
            884700000000000001L;

    private static final Long REGION_ID = 53390L;

    private static final Long AUTHOR_MEMBER_ID = 1001L;

    private static final Long INITIAL_VERSION = 0L;

    private static final Long REVISION_ID = 1L;

    private static final String CREATE_REASON =
            "매물 등록으로 초기 상태가 설정되었습니다.";

    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);

    private final PropertyRevisionRepository
            propertyRevisionRepository =
            mock(PropertyRevisionRepository.class);

    private final PropertyStatusHistoryRepository
            propertyStatusHistoryRepository =
            mock(PropertyStatusHistoryRepository.class);

    private final PropertyPublisherSnapshotRepository
            propertyPublisherSnapshotRepository =
            mock(PropertyPublisherSnapshotRepository.class);

    private final RegionRepository regionRepository =
            mock(RegionRepository.class);

    private final PropertyPricePolicy propertyPricePolicy =
            mock(PropertyPricePolicy.class);

    private final TsidGenerator tsidGenerator =
            mock(TsidGenerator.class);

    private final ObjectMapper objectMapper =
            mock(ObjectMapper.class);

    private final PropertyAuditEventRecorder
            propertyAuditEventRecorder =
            mock(PropertyAuditEventRecorder.class);

    private final PropertyKafkaEventPublisher
            propertyKafkaEventPublisher =
            mock(PropertyKafkaEventPublisher.class);

    private final PropertyAddressService propertyAddressService =
            mock(PropertyAddressService.class);

    private final PropertyCreateService propertyCreateService =
            new PropertyCreateService(
                    propertyRepository,
                    propertyRevisionRepository,
                    propertyStatusHistoryRepository,
                    propertyPublisherSnapshotRepository,
                    regionRepository,
                    propertyPricePolicy,
                    tsidGenerator,
                    objectMapper,
                    propertyAuditEventRecorder,
                    propertyKafkaEventPublisher,
                    propertyAddressService
            );

    private final ActorContext ownerContext =
            ActorContext.member(
                    AUTHOR_MEMBER_ID,
                    ActorRole.USER,
                    "property-create-test-owner"
            );

    @Test
    void create_validOwnerRequest_savesPropertyAndRelatedRecords() {
        PropertyCreateCommand command =
                createValidCommand();

        when(
                regionRepository
                        .findByRegionIdAndIsActiveTrue(REGION_ID)
        ).thenReturn(
                Optional.of(mock(Region.class))
        );

        when(tsidGenerator.generate())
                .thenReturn(PROPERTY_ID);

        PreparedPropertyAddress preparedAddress =
                mock(PreparedPropertyAddress.class);

        when(preparedAddress.regionId())
                .thenReturn(REGION_ID);

        when(
                propertyAddressService.prepare(
                        eq(PROPERTY_ID),
                        same(command.address())
                )
        ).thenReturn(preparedAddress);

        when(
                propertyRepository.saveAndFlush(
                        any(Property.class)
                )
        ).thenAnswer(invocation -> {
            Property property = invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    property,
                    "version",
                    INITIAL_VERSION
            );

            return property;
        });

        when(
                propertyRevisionRepository.save(
                        any(PropertyRevision.class)
                )
        ).thenAnswer(invocation -> {
            PropertyRevision revision =
                    invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    revision,
                    "propertyRevisionId",
                    REVISION_ID
            );

            return revision;
        });

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{}");

        PropertyCreateResponse response =
                propertyCreateService.create(
                        command,
                        ownerContext
                );

        assertThat(response.propertyId())
                .isEqualTo(PROPERTY_ID);

        assertThat(response.version())
                .isEqualTo(INITIAL_VERSION);

        verify(propertyPricePolicy)
                .validate(
                        command.transactionType(),
                        command.salePrice(),
                        command.deposit(),
                        command.monthlyRent()
                );

        verify(propertyRepository)
                .saveAndFlush(any(Property.class));

        verify(propertyAddressService)
                .create(
                        any(Property.class),
                        same(preparedAddress),
                        same(ownerContext)
                );

        verify(propertyRevisionRepository)
                .save(any(PropertyRevision.class));

        verify(propertyStatusHistoryRepository)
                .saveAll(anyList());

        verify(propertyPublisherSnapshotRepository)
                .save(any(PropertyPublisherSnapshot.class));

        ArgumentCaptor<Instant> auditOccurredAtCaptor =
                ArgumentCaptor.forClass(Instant.class);

        verify(propertyAuditEventRecorder)
                .recordPropertyAction(
                        eq(PROPERTY_ID),
                        eq(
                                PropertyAuditActionCode
                                        .PROPERTY_CREATED
                        ),
                        eq(CREATE_REASON),
                        isNull(),
                        auditOccurredAtCaptor.capture(),
                        same(ownerContext)
                );

        ArgumentCaptor<Instant> kafkaOccurredAtCaptor =
                ArgumentCaptor.forClass(Instant.class);

        verify(propertyKafkaEventPublisher)
                .publishAfterCommit(
                        eq(PROPERTY_ID),
                        eq(INITIAL_VERSION),
                        eq(
                                PropertyEventType
                                        .PROPERTY_CREATED
                        ),
                        argThat(payload ->
                                PROPERTY_ID
                                        .toString()
                                        .equals(
                                                payload.get(
                                                        "propertyId"
                                                )
                                        )
                                        && INITIAL_VERSION.equals(
                                        payload.get(
                                                "version"
                                        )
                                )
                                        && REGION_ID
                                        .toString()
                                        .equals(
                                                payload.get(
                                                        "regionId"
                                                )
                                        )
                                        && payload.get("deposit")
                                        == null
                                        && payload.get(
                                        "monthlyRent"
                                ) == null
                        ),
                        kafkaOccurredAtCaptor.capture(),
                        same(ownerContext)
                );

        assertThat(kafkaOccurredAtCaptor.getValue())
                .isEqualTo(
                        auditOccurredAtCaptor.getValue()
                );
    }

    @Test
    void create_inactiveRegion_throwsAndDoesNotRecordEvents() {
        PropertyCreateCommand command =
                createValidCommand();

        when(
                regionRepository
                        .findByRegionIdAndIsActiveTrue(REGION_ID)
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> propertyCreateService.create(
                        command,
                        ownerContext
                )
        ).isInstanceOf(BusinessException.class);

        verify(propertyRepository, never())
                .saveAndFlush(any(Property.class));

        verify(propertyAuditEventRecorder, never())
                .recordPropertyAction(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );

        verify(propertyKafkaEventPublisher, never())
                .publishAfterCommit(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void create_roleAndPublisherTypeMismatch_throwsAndDoesNotSave() {
        PropertyCreateCommand command =
                createValidCommand();

        ActorContext agentContext =
                ActorContext.member(
                        2002L,
                        ActorRole.AGENT,
                        "property-create-test-agent"
                );

        assertThatThrownBy(
                () -> propertyCreateService.create(
                        command,
                        agentContext
                )
        ).isInstanceOf(BusinessException.class);

        verify(regionRepository, never())
                .findByRegionIdAndIsActiveTrue(any());

        verify(propertyRepository, never())
                .saveAndFlush(any(Property.class));

        verify(propertyAuditEventRecorder, never())
                .recordPropertyAction(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );

        verify(propertyKafkaEventPublisher, never())
                .publishAfterCommit(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    private PropertyCreateCommand createValidCommand() {
        return new PropertyCreateCommand(
                REGION_ID,
                null,
                PublisherType.DIRECT_OWNER,
                PropertyType.APARTMENT,
                com.zipdaproperty
                        .domain
                        .property
                        .constant
                        .TransactionType
                        .SALE,
                500_000_000L,
                null,
                null,
                150_000L,
                new BigDecimal("84.99"),
                new BigDecimal("59.99"),
                3,
                1,
                5,
                20,
                "중층",
                "남향",
                LocalDate.of(
                        2020,
                        1,
                        1
                ),
                "공동주택",
                true,
                true,
                false,
                "Kafka 등록 테스트 매물",
                "매물 등록과 감사 및 Kafka 발행을 검증합니다.",
                new PropertyAddressCommand(
                        "대구 수성구 달구벌대로 2450",
                        "대구광역시 수성구 범어동 123",
                        "2726010100",
                        new BigDecimal("128.625123"),
                        new BigDecimal("35.859321")
                )
        );
    }
}
