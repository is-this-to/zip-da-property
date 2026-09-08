package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.command.PropertyUpdateCommand;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.RevisionChangeType;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.request.PropertyUpdateRequest;
import com.zipdaproperty.domain.property.response.PropertyUpdateResponse;
import com.zipdaproperty.domain.region.entity.Region;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyUpdateServiceTest {

    private static final Long PROPERTY_ID =
            884685586571263701L;

    private static final Long REGION_ID = 53390L;

    private static final Long AUTHOR_MEMBER_ID = 1001L;

    private static final Long OTHER_MEMBER_ID = 2002L;

    private static final Long CURRENT_VERSION = 0L;

    private static final Long NEXT_VERSION = 1L;

    private static final String BEFORE_TITLE =
            "수정 전 제목";

    private static final String AFTER_TITLE =
            "수정 후 제목";

    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);

    private final PropertyRevisionRepository
            propertyRevisionRepository =
            mock(PropertyRevisionRepository.class);

    private final RegionRepository regionRepository =
            mock(RegionRepository.class);

    private final PropertyVersionPolicy propertyVersionPolicy =
            new PropertyVersionPolicy();

    private final PropertyUpdateCommandFactory
            propertyUpdateCommandFactory =
            mock(PropertyUpdateCommandFactory.class);

    private final PropertyUpdatePolicy propertyUpdatePolicy =
            mock(PropertyUpdatePolicy.class);

    private final PropertyPricePolicy propertyPricePolicy =
            mock(PropertyPricePolicy.class);

    private final ObjectMapper objectMapper =
            mock(ObjectMapper.class);

    private final PropertyUpdateService propertyUpdateService =
            new PropertyUpdateService(
                    propertyRepository,
                    propertyRevisionRepository,
                    regionRepository,
                    propertyVersionPolicy,
                    propertyUpdateCommandFactory,
                    propertyUpdatePolicy,
                    propertyPricePolicy,
                    objectMapper
            );

    private final ActorContext ownerContext =
            ActorContext.member(
                    AUTHOR_MEMBER_ID,
                    ActorRole.USER,
                    "property-update-test-owner"
            );

    @Test
    void update_ownerAndValidVersion_savesPropertyAndRevision() {
        Property property =
                prepareExistingProperty(CURRENT_VERSION);

        PropertyUpdateRequest request =
                createTitleUpdateRequest(
                        CURRENT_VERSION
                );

        PropertyUpdateCommand command =
                createUpdateCommand(
                        CURRENT_VERSION,
                        AFTER_TITLE
                );

        when(property.getVersion())
                .thenReturn(
                        CURRENT_VERSION,
                        NEXT_VERSION,
                        NEXT_VERSION
                );

        when(property.getPropertyId())
                .thenReturn(PROPERTY_ID);

        when(property.getTitle())
                .thenReturn(BEFORE_TITLE);

        when(property.getPublicationStatus())
                .thenReturn(PublicationStatus.IN_REVIEW);

        when(property.getTransactionStatus())
                .thenReturn(TransactionStatus.AVAILABLE);

        when(property.getVerificationStatus())
                .thenReturn(VerificationStatus.UNVERIFIED);

        when(
                propertyUpdateCommandFactory.create(
                        property,
                        request
                )
        ).thenReturn(command);

        when(
                regionRepository
                        .findByRegionIdAndIsActiveTrue(REGION_ID)
        ).thenReturn(
                Optional.of(mock(Region.class))
        );

        when(propertyRepository.saveAndFlush(property))
                .thenReturn(property);

        when(objectMapper.writeValueAsString(property))
                .thenReturn(
                        "{\"title\":\"수정 전 제목\"}",
                        "{\"title\":\"수정 후 제목\"}"
                );

        when(
                objectMapper.writeValueAsString(
                        List.of("title")
                )
        ).thenReturn("[\"title\"]");

        PropertyUpdateResponse response =
                propertyUpdateService.update(
                        PROPERTY_ID,
                        request,
                        ownerContext
                );

        assertThat(response.propertyId())
                .isEqualTo(PROPERTY_ID);

        assertThat(response.version())
                .isEqualTo(NEXT_VERSION);

        assertThat(response.publicationStatus())
                .isEqualTo(PublicationStatus.IN_REVIEW);

        assertThat(response.transactionStatus())
                .isEqualTo(TransactionStatus.AVAILABLE);

        assertThat(response.verificationStatus())
                .isEqualTo(VerificationStatus.UNVERIFIED);

        verify(propertyUpdatePolicy)
                .validate(command);

        verify(propertyPricePolicy)
                .validate(
                        TransactionType.SALE,
                        500_000_000L,
                        null,
                        null
                );

        verify(property)
                .update(
                        command,
                        ownerContext
                );

        verify(propertyRepository)
                .saveAndFlush(property);

        ArgumentCaptor<PropertyRevision> revisionCaptor =
                ArgumentCaptor.forClass(
                        PropertyRevision.class
                );

        verify(propertyRevisionRepository)
                .save(revisionCaptor.capture());

        PropertyRevision savedRevision =
                revisionCaptor.getValue();

        assertThat(savedRevision.getPropertyId())
                .isEqualTo(PROPERTY_ID);

        assertThat(savedRevision.getPropertyVersion())
                .isEqualTo(NEXT_VERSION);

        assertThat(savedRevision.getChangeType())
                .isEqualTo(RevisionChangeType.UPDATE);

        assertThat(savedRevision.getChangedFieldsJson())
                .isEqualTo("[\"title\"]");

        assertThat(savedRevision.getActorMemberId())
                .isEqualTo(AUTHOR_MEMBER_ID);
    }

    @Test
    void update_staleVersion_throwsVersionConflict() {
        Property property =
                prepareExistingProperty(NEXT_VERSION);

        PropertyUpdateRequest request =
                createTitleUpdateRequest(
                        CURRENT_VERSION
                );

        assertThatThrownBy(
                () -> propertyUpdateService.update(
                        PROPERTY_ID,
                        request,
                        ownerContext
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode.VERSION_CONFLICT
                        )
        );

        verify(
                propertyUpdateCommandFactory,
                never()
        ).create(
                any(Property.class),
                any(PropertyUpdateRequest.class)
        );

        verify(
                propertyRepository,
                never()
        ).saveAndFlush(
                any(Property.class)
        );

        verify(
                propertyRevisionRepository,
                never()
        ).save(
                any(PropertyRevision.class)
        );
    }

    @Test
    void update_differentMember_throwsOwnershipRequired() {
        Property property =
                prepareExistingProperty(NEXT_VERSION);

        PropertyUpdateRequest request =
                createTitleUpdateRequest(
                        NEXT_VERSION
                );

        ActorContext otherMemberContext =
                ActorContext.member(
                        OTHER_MEMBER_ID,
                        ActorRole.USER,
                        "property-update-test-other-member"
                );

        assertThatThrownBy(
                () -> propertyUpdateService.update(
                        PROPERTY_ID,
                        request,
                        otherMemberContext
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode
                                        .PROPERTY_OWNERSHIP_REQUIRED
                        )
        );

        verify(
                propertyUpdateCommandFactory,
                never()
        ).create(
                any(Property.class),
                any(PropertyUpdateRequest.class)
        );

        verify(
                propertyRepository,
                never()
        ).saveAndFlush(
                any(Property.class)
        );

        verify(
                propertyRevisionRepository,
                never()
        ).save(
                any(PropertyRevision.class)
        );
    }

    @Test
    void update_optimisticLockFailure_throwsVersionConflict() {
        Property property =
                prepareExistingProperty(CURRENT_VERSION);

        PropertyUpdateRequest request =
                createTitleUpdateRequest(
                        CURRENT_VERSION
                );

        PropertyUpdateCommand command =
                createUpdateCommand(
                        CURRENT_VERSION,
                        AFTER_TITLE
                );

        when(property.getTitle())
                .thenReturn(BEFORE_TITLE);

        when(
                propertyUpdateCommandFactory.create(
                        property,
                        request
                )
        ).thenReturn(command);

        when(
                regionRepository
                        .findByRegionIdAndIsActiveTrue(REGION_ID)
        ).thenReturn(
                Optional.of(mock(Region.class))
        );

        when(objectMapper.writeValueAsString(property))
                .thenReturn(
                        "{\"title\":\"수정 전 제목\"}"
                );

        when(propertyRepository.saveAndFlush(property))
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "동시 수정 충돌 테스트"
                        )
                );

        assertThatThrownBy(
                () -> propertyUpdateService.update(
                        PROPERTY_ID,
                        request,
                        ownerContext
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode.VERSION_CONFLICT
                        )
        );

        verify(property)
                .update(
                        command,
                        ownerContext
                );

        verify(
                propertyRevisionRepository,
                never()
        ).save(
                any(PropertyRevision.class)
        );
    }

    private Property prepareExistingProperty(
            Long currentVersion
    ) {
        Property property =
                mock(Property.class);

        when(
                propertyRepository
                        .findByPropertyIdAndDeletedAtIsNull(
                                PROPERTY_ID
                        )
        ).thenReturn(Optional.of(property));

        when(property.getAuthorMemberId())
                .thenReturn(AUTHOR_MEMBER_ID);

        when(property.getVersion())
                .thenReturn(currentVersion);

        return property;
    }

    private PropertyUpdateRequest createTitleUpdateRequest(
            Long requestedVersion
    ) {
        JsonNode titleNode =
                mock(JsonNode.class);

        return new PropertyUpdateRequest(
                requestedVersion,
                Map.of(
                        "title",
                        titleNode
                )
        );
    }

    private PropertyUpdateCommand createUpdateCommand(
            Long requestedVersion,
            String title
    ) {
        return new PropertyUpdateCommand(
                requestedVersion,
                REGION_ID,
                null,
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
                LocalDate.of(2020, 1, 1),
                "공동주택",
                true,
                true,
                false,
                title,
                "자동 테스트용 매물 설명"
        );
    }
}