package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.image.service.PropertyImageLinkService;
import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
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
import org.mockito.InOrder;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyCreateServiceTest {

    private static final Long PROPERTY_ID = 100L;
    private static final ActorContext ACTOR = ActorContext.member(200L, ActorRole.USER, "property-create-test");
    private static final List<Long> FILE_IDS = List.of(1003L, 1001L, 1002L);

    private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
    private final PropertyRevisionRepository revisionRepository = mock(PropertyRevisionRepository.class);
    private final PropertyStatusHistoryRepository historyRepository = mock(PropertyStatusHistoryRepository.class);
    private final PropertyPublisherSnapshotRepository snapshotRepository = mock(PropertyPublisherSnapshotRepository.class);
    private final RegionRepository regionRepository = mock(RegionRepository.class);
    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);
    private final PropertyImageLinkService imageLinkService = mock(PropertyImageLinkService.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final SimpleTransactionStatus transactionStatus = new SimpleTransactionStatus();

    private PropertyCreateService service;

    @BeforeEach
    void setUp() {
        PropertyCreateService target = new PropertyCreateService(
                propertyRepository, revisionRepository, historyRepository, snapshotRepository,
                regionRepository, new PropertyPricePolicy(), tsidGenerator,
                JsonMapper.builder().build(), imageLinkService
        );
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactionManager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.addAdvice(interceptor);
        service = (PropertyCreateService) proxyFactory.getProxy();

        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(regionRepository.findByRegionIdAndIsActiveTrue(10L)).thenReturn(Optional.of(mock(Region.class)));
        when(tsidGenerator.generate()).thenReturn(PROPERTY_ID);
        when(propertyRepository.saveAndFlush(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(revisionRepository.save(any(PropertyRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_validImages_linksBeforeHistoriesAndCommitsRegistration() {
        PropertyCreateResponse response = service.create(command(), ACTOR);

        assertThat(response.propertyId()).isEqualTo(PROPERTY_ID);
        Property expected = Property.create(PROPERTY_ID, command(), ACTOR);
        assertThat(response).isEqualTo(PropertyCreateResponse.from(expected));
        InOrder order = inOrder(propertyRepository, imageLinkService, revisionRepository,
                historyRepository, snapshotRepository, transactionManager);
        order.verify(propertyRepository).saveAndFlush(any(Property.class));
        order.verify(imageLinkService).linkImages(eq(PROPERTY_ID), same(FILE_IDS), same(ACTOR));
        order.verify(revisionRepository).save(any(PropertyRevision.class));
        order.verify(historyRepository).saveAll(argThat(histories -> ((List<?>) histories).size() == 3));
        order.verify(snapshotRepository).save(any());
        order.verify(transactionManager).commit(transactionStatus);
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    void create_imageLinkFailure_rollsBackAndSkipsHistories() {
        BusinessException failure = new BusinessException(CustomResponseCode.INVALID_REQUEST, "이미지 연결 실패");
        doThrow(failure).when(imageLinkService).linkImages(PROPERTY_ID, FILE_IDS, ACTOR);

        assertThatThrownBy(() -> service.create(command(), ACTOR)).isSameAs(failure);

        verify(propertyRepository).saveAndFlush(any(Property.class));
        verify(imageLinkService).linkImages(eq(PROPERTY_ID), same(FILE_IDS), same(ACTOR));
        verifyNoInteractions(revisionRepository, historyRepository, snapshotRepository);
        verify(transactionManager).getTransaction(argThat(definition ->
                definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED));
        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void create_snapshotFailureAfterImageLink_rollsBackRegistration() {
        IllegalStateException failure = new IllegalStateException("스냅샷 저장 실패");
        when(snapshotRepository.save(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.create(command(), ACTOR)).isSameAs(failure);

        verify(imageLinkService).linkImages(eq(PROPERTY_ID), same(FILE_IDS), same(ACTOR));
        verify(revisionRepository).save(any(PropertyRevision.class));
        verify(historyRepository).saveAll(anyList());
        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void create_unauthorizedActor_doesNotSaveOrLinkImages() {
        assertThatThrownBy(() -> service.create(command(), ActorContext.system("property-create-denied")))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(propertyRepository, imageLinkService, revisionRepository,
                historyRepository, snapshotRepository);
    }

    private PropertyCreateCommand command() {
        return new PropertyCreateCommand(
                10L, null, PublisherType.DIRECT_OWNER, PropertyType.APARTMENT, TransactionType.SALE,
                100000L, null, null, null, null, new BigDecimal("84.00"),
                null, null, null, null, null, null, null, null, null, null, null,
                "등록 테스트", "매물 등록 테스트 설명", FILE_IDS
        );
    }
}
