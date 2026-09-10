package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.image.service.PropertyImageLinkService;
import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyPublisherSnapshot;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
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
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyCreateServiceTest {

    private static final Long PROPERTY_ID =
            884700000000000001L;

    private static final Long REGION_ID = 53390L;

    private static final Long AUTHOR_MEMBER_ID = 1001L;

    private static final Long INITIAL_VERSION = 0L;

    private static final Long REVISION_ID = 1L;

    private static final List<Long> FILE_IDS =
            List.of(
                    1003L,
                    1001L,
                    1002L
            );

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

    private final PropertyImageLinkService
            propertyImageLinkService =
            mock(PropertyImageLinkService.class);

    private final PropertyAuditEventRecorder
            propertyAuditEventRecorder =
            mock(PropertyAuditEventRecorder.class);

    private final PropertyKafkaEventPublisher
            propertyKafkaEventPublisher =
            mock(PropertyKafkaEventPublisher.class);

    private final PlatformTransactionManager
            transactionManager =
            mock(PlatformTransactionManager.class);

    private final SimpleTransactionStatus
            transactionStatus =
            new SimpleTransactionStatus();

    private PropertyCreateService propertyCreateService;

    private final ActorContext ownerContext =
            ActorContext.member(
                    AUTHOR_MEMBER_ID,
                    ActorRole.USER,
                    "property-create-test-owner"
            );

    @BeforeEach
    void setUp() {
        PropertyCreateService target =
                new PropertyCreateService(
                        propertyRepository,
                        propertyRevisionRepository,
                        propertyStatusHistoryRepository,
                        propertyPublisherSnapshotRepository,
                        regionRepository,
                        propertyPricePolicy,
                        tsidGenerator,
                        objectMapper,
                        propertyImageLinkService,
                        propertyAuditEventRecorder,
                        propertyKafkaEventPublisher
                );

        TransactionInterceptor interceptor =
                new TransactionInterceptor();

        interceptor.setTransactionManager(
                transactionManager
        );

        interceptor.setTransactionAttributeSource(
                new AnnotationTransactionAttributeSource()
        );

        ProxyFactory proxyFactory =
                new ProxyFactory(target);

        proxyFactory.addAdvice(interceptor);

        propertyCreateService =
                (PropertyCreateService) proxyFactory.getProxy();

        when(transactionManager.getTransaction(any()))
                .thenReturn(transactionStatus);
    }

    @Test
    void create_validOwnerRequest_savesPropertyImagesAndRelatedRecords() {
        PropertyCreateCommand command =
                createValidCommand();

        stubValidCreatePersistence();

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

        InOrder order =
                inOrder(
                        propertyRepository,
                        propertyImageLinkService,
                        propertyRevisionRepository,
                        propertyStatusHistoryRepository,
                        propertyPublisherSnapshotRepository,
                        transactionManager
                );

        order.verify(propertyRepository)
                .saveAndFlush(any(Property.class));

        order.verify(propertyImageLinkService)
                .linkImages(
                        eq(PROPERTY_ID),
                        same(FILE_IDS),
                        same(ownerContext)
                );

        order.verify(propertyRevisionRepository)
                .save(any(PropertyRevision.class));

        order.verify(propertyStatusHistoryRepository)
                .saveAll(anyList());

        order.verify(propertyPublisherSnapshotRepository)
                .save(any(PropertyPublisherSnapshot.class));

        order.verify(transactionManager)
                .commit(transactionStatus);

        verify(transactionManager, never())
                .rollback(any());

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
    void create_imageLinkFailure_rollsBackAndSkipsRelatedRecords() {
        PropertyCreateCommand command =
                createValidCommand();

        stubValidCreatePersistence();

        BusinessException failure =
                new BusinessException(
                        CustomResponseCode.INVALID_REQUEST,
                        "이미지 연결 실패"
                );

        doThrow(failure)
                .when(propertyImageLinkService)
                .linkImages(
                        eq(PROPERTY_ID),
                        same(FILE_IDS),
                        same(ownerContext)
                );

        assertThatThrownBy(
                () -> propertyCreateService.create(
                        command,
                        ownerContext
                )
        ).isSameAs(failure);

        verify(propertyRepository)
                .saveAndFlush(any(Property.class));

        verify(propertyImageLinkService)
                .linkImages(
                        eq(PROPERTY_ID),
                        same(FILE_IDS),
                        same(ownerContext)
                );

        verifyNoInteractions(
                propertyRevisionRepository,
                propertyStatusHistoryRepository,
                propertyPublisherSnapshotRepository,
                propertyAuditEventRecorder,
                propertyKafkaEventPublisher
        );

        verify(transactionManager)
                .rollback(transactionStatus);

        verify(transactionManager, never())
                .commit(any());
    }

    @Test
    void create_snapshotFailureAfterImageLink_rollsBackRegistration() {
        PropertyCreateCommand command =
                createValidCommand();

        stubValidCreatePersistence();

        IllegalStateException failure =
                new IllegalStateException(
                        "스냅샷 저장 실패"
                );

        when(
                propertyPublisherSnapshotRepository.save(
                        any(PropertyPublisherSnapshot.class)
                )
        ).thenThrow(failure);

        assertThatThrownBy(
                () -> propertyCreateService.create(
                        command,
                        ownerContext
                )
        ).isSameAs(failure);

        verify(propertyImageLinkService)
                .linkImages(
                        eq(PROPERTY_ID),
                        same(FILE_IDS),
                        same(ownerContext)
                );

        verify(propertyRevisionRepository)
                .save(any(PropertyRevision.class));

        verify(propertyStatusHistoryRepository)
                .saveAll(anyList());

        verify(propertyPublisherSnapshotRepository)
                .save(any(PropertyPublisherSnapshot.class));

        verifyNoInteractions(
                propertyAuditEventRecorder,
                propertyKafkaEventPublisher
        );

        verify(transactionManager)
                .rollback(transactionStatus);

        verify(transactionManager, never())
                .commit(any());
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

        verify(propertyImageLinkService, never())
                .linkImages(
                        any(),
                        anyList(),
                        any()
                );

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

        verify(transactionManager)
                .rollback(transactionStatus);

        verify(transactionManager, never())
                .commit(any());
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

        verify(propertyImageLinkService, never())
                .linkImages(
                        any(),
                        anyList(),
                        any()
                );

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

        verify(transactionManager)
                .rollback(transactionStatus);

        verify(transactionManager, never())
                .commit(any());
    }

    private void stubValidCreatePersistence() {
        when(
                regionRepository
                        .findByRegionIdAndIsActiveTrue(REGION_ID)
        ).thenReturn(
                Optional.of(mock(Region.class))
        );

        when(tsidGenerator.generate())
                .thenReturn(PROPERTY_ID);

        when(
                propertyRepository.saveAndFlush(
                        any(Property.class)
                )
        ).thenAnswer(invocation -> {
            Property property =
                    invocation.getArgument(0);

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
    }

    private PropertyCreateCommand createValidCommand() {
        return new PropertyCreateCommand(
                REGION_ID,
                null,
                PublisherType.DIRECT_OWNER,
                PropertyType.APARTMENT,
                TransactionType.SALE,
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
                FILE_IDS
        );
    }
}