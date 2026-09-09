package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.RevisionChangeScope;
import com.zipdaproperty.domain.property.constant.RevisionChangeType;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.request.PropertyDeleteRequest;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;
import tools.jackson.databind.ObjectMapper;

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

class PropertyDeleteServiceTest {

    private static final Long PROPERTY_ID =
            884685586571263701L;

    private static final Long AUTHOR_MEMBER_ID =
            1001L;

    private static final Long OTHER_MEMBER_ID =
            2002L;

    private static final Long ADMIN_MEMBER_ID =
            3003L;

    private static final Long CURRENT_VERSION =
            2L;

    private static final Long NEXT_VERSION =
            3L;

    private static final String DELETE_REASON =
            "작성자가 매물 게시를 중단했습니다.";

    private static final List<String> DELETE_CHANGED_FIELDS =
            List.of(
                    "deletedAt",
                    "deletedByMemberId",
                    "deletedByRole",
                    "deleteReason"
            );

    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);

    private final PropertyRevisionRepository
            propertyRevisionRepository =
            mock(PropertyRevisionRepository.class);

    private final PropertyVersionPolicy propertyVersionPolicy =
            new PropertyVersionPolicy();

    private final ObjectMapper objectMapper =
            mock(ObjectMapper.class);

    private final PropertyDeleteService propertyDeleteService =
            new PropertyDeleteService(
                    propertyRepository,
                    propertyRevisionRepository,
                    propertyVersionPolicy,
                    objectMapper
            );

    private final ActorContext ownerContext =
            ActorContext.member(
                    AUTHOR_MEMBER_ID,
                    ActorRole.USER,
                    "property-delete-test-owner"
            );

    private final ActorContext adminContext =
            ActorContext.member(
                    ADMIN_MEMBER_ID,
                    ActorRole.CS_ADMIN,
                    "property-delete-test-admin"
            );

    @Test
    void delete_ownerAndValidVersion_savesPropertyAndRevision() {
        Property property =
                prepareExistingProperty(
                        CURRENT_VERSION
                );

        PropertyDeleteRequest request =
                createRequest(CURRENT_VERSION);

        when(property.getPropertyId())
                .thenReturn(PROPERTY_ID);

        when(property.getVersion())
                .thenReturn(
                        CURRENT_VERSION,
                        NEXT_VERSION
                );

        when(objectMapper.writeValueAsString(property))
                .thenReturn(
                        "{\"deletedAt\":null}",
                        "{\"deletedAt\":\"deleted\"}"
                );

        when(
                objectMapper.writeValueAsString(
                        DELETE_CHANGED_FIELDS
                )
        ).thenReturn(
                """
                [
                  "deletedAt",
                  "deletedByMemberId",
                  "deletedByRole",
                  "deleteReason"
                ]
                """
        );

        when(propertyRepository.saveAndFlush(property))
                .thenReturn(property);

        propertyDeleteService.delete(
                PROPERTY_ID,
                request,
                ownerContext
        );

        ArgumentCaptor<Instant> occurredAtCaptor =
                ArgumentCaptor.forClass(Instant.class);

        verify(property)
                .softDelete(
                        eq(ownerContext),
                        occurredAtCaptor.capture(),
                        eq(DELETE_REASON)
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
                .isEqualTo(
                        RevisionChangeType.SOFT_DELETE
                );

        assertThat(savedRevision.getChangeScope())
                .isEqualTo(
                        RevisionChangeScope.PROPERTY
                );

        assertThat(savedRevision.getBeforeSnapshotJson())
                .isEqualTo(
                        "{\"deletedAt\":null}"
                );

        assertThat(savedRevision.getAfterSnapshotJson())
                .isEqualTo(
                        "{\"deletedAt\":\"deleted\"}"
                );

        assertThat(savedRevision.getChangeReason())
                .isEqualTo(DELETE_REASON);

        assertThat(savedRevision.getActorMemberId())
                .isEqualTo(AUTHOR_MEMBER_ID);

        assertThat(savedRevision.getActorRole())
                .isEqualTo(ActorRole.USER);

        assertThat(savedRevision.getOccurredAt())
                .isEqualTo(
                        occurredAtCaptor.getValue()
                );
    }

    @Test
    void delete_csAdmin_succeedsWithoutOwnership() {
        Property property =
                prepareExistingProperty(
                        CURRENT_VERSION
                );

        PropertyDeleteRequest request =
                createRequest(CURRENT_VERSION);

        when(property.getPropertyId())
                .thenReturn(PROPERTY_ID);

        when(property.getVersion())
                .thenReturn(
                        CURRENT_VERSION,
                        NEXT_VERSION
                );

        when(objectMapper.writeValueAsString(property))
                .thenReturn(
                        "{\"deletedAt\":null}",
                        "{\"deletedAt\":\"deleted\"}"
                );

        when(
                objectMapper.writeValueAsString(
                        DELETE_CHANGED_FIELDS
                )
        ).thenReturn(
                "[\"deletedAt\"]"
        );

        when(propertyRepository.saveAndFlush(property))
                .thenReturn(property);

        propertyDeleteService.delete(
                PROPERTY_ID,
                request,
                adminContext
        );

        verify(property)
                .softDelete(
                        eq(adminContext),
                        any(Instant.class),
                        eq(DELETE_REASON)
                );

        verify(propertyRepository)
                .saveAndFlush(property);

        verify(propertyRevisionRepository)
                .save(
                        any(PropertyRevision.class)
                );
    }

    @Test
    void delete_staleVersion_throwsVersionConflict() {
        Property property =
                prepareExistingProperty(
                        NEXT_VERSION
                );

        PropertyDeleteRequest request =
                createRequest(CURRENT_VERSION);

        assertThatThrownBy(
                () -> propertyDeleteService.delete(
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
                property,
                never()
        ).softDelete(
                any(ActorContext.class),
                any(Instant.class),
                any(String.class)
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
    void delete_differentMember_throwsOwnershipRequired() {
        Property property =
                prepareExistingProperty(
                        CURRENT_VERSION
                );

        PropertyDeleteRequest request =
                createRequest(CURRENT_VERSION);

        ActorContext otherMemberContext =
                ActorContext.member(
                        OTHER_MEMBER_ID,
                        ActorRole.USER,
                        "property-delete-test-other-member"
                );

        assertThatThrownBy(
                () -> propertyDeleteService.delete(
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
                property,
                never()
        ).softDelete(
                any(ActorContext.class),
                any(Instant.class),
                any(String.class)
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
    void delete_optimisticLockFailure_throwsVersionConflict() {
        Property property =
                prepareExistingProperty(
                        CURRENT_VERSION
                );

        PropertyDeleteRequest request =
                createRequest(CURRENT_VERSION);

        when(objectMapper.writeValueAsString(property))
                .thenReturn(
                        "{\"deletedAt\":null}"
                );

        when(propertyRepository.saveAndFlush(property))
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "매물 삭제 동시 변경 충돌 테스트"
                        )
                );

        assertThatThrownBy(
                () -> propertyDeleteService.delete(
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
                .softDelete(
                        eq(ownerContext),
                        any(Instant.class),
                        eq(DELETE_REASON)
                );

        verify(propertyRepository)
                .saveAndFlush(property);

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
        ).thenReturn(
                Optional.of(property)
        );

        when(property.getAuthorMemberId())
                .thenReturn(AUTHOR_MEMBER_ID);

        when(property.getVersion())
                .thenReturn(currentVersion);

        return property;
    }

    private PropertyDeleteRequest createRequest(
            Long version
    ) {
        return new PropertyDeleteRequest(
                version,
                DELETE_REASON
        );
    }
}