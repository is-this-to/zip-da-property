package com.zipdaproperty.domain.option.service;

import com.zipdaproperty.domain.option.command.PropertyOptionCreateCommand;
import com.zipdaproperty.domain.option.entity.PropertyOption;
import com.zipdaproperty.domain.option.entity.PropertyOptionCode;
import com.zipdaproperty.domain.option.entity.PropertyOptionHistory;
import com.zipdaproperty.domain.option.entity.PropertyTypeOption;
import com.zipdaproperty.domain.option.repository.PropertyOptionHistoryRepository;
import com.zipdaproperty.domain.option.repository.PropertyOptionQueryDSLRepository;
import com.zipdaproperty.domain.option.repository.PropertyOptionRepository;
import com.zipdaproperty.domain.option.type.OptionChangeType;
import com.zipdaproperty.domain.option.validator.OptionValueValidator;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.OptionCodeNotFoundException;
import com.zipdaproperty.global.error.custom.business.OptionNotAllowedForPropertyTypeException;
import com.zipdaproperty.global.error.custom.business.OptionValueInvalidException;
import com.zipdaproperty.global.error.custom.business.OptionValueRequiredException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyOptionCommandServiceTest {

    private static final Long PROPERTY_ID = 100L;
    private static final Long REVISION_ID = 200L;
    private static final String CHANGED_FIELDS = "optionValue,displayOrder";
    private static final String REMOVED_REASON = "OPTION_REMOVED_FROM_REQUEST";
    private static final PropertyType PROPERTY_TYPE = PropertyType.APARTMENT;
    private static final ActorContext ACTOR_CONTEXT = ActorContext.member(
            300L,
            ActorRole.USER,
            "property-option-command-test"
    );

    private final PropertyOptionRepository propertyOptionRepository =
            mock(PropertyOptionRepository.class);
    private final PropertyOptionHistoryRepository historyRepository =
            mock(PropertyOptionHistoryRepository.class);
    private final PropertyOptionQueryDSLRepository queryRepository =
            mock(PropertyOptionQueryDSLRepository.class);

    private PropertyOptionCommandService service;

    @BeforeEach
    void setUp() {
        service = new PropertyOptionCommandService(
                propertyOptionRepository,
                historyRepository,
                queryRepository,
                new OptionValueValidator()
        );
        when(queryRepository.findActiveOptionsByPropertyId(PROPERTY_ID))
                .thenReturn(List.of());

        AtomicLong optionId = new AtomicLong(1_000L);
        when(propertyOptionRepository.save(any(PropertyOption.class)))
                .thenAnswer(invocation -> {
                    PropertyOption option = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            option,
                            "propertyOptionId",
                            optionId.getAndIncrement()
                    );
                    return option;
                });
    }

    @Test
    void prepareSync_newOption_requiresChanges() {
        preparePolicies(
                List.of(typeOption(10L, false, 3)),
                List.of(optionCode(10L, "ELEVATOR", true))
        );

        assertThat(prepareSync(List.of(command("ELEVATOR", "true")))
                .changesRequired()).isTrue();
    }

    @Test
    void prepareSync_changedValue_requiresChanges() {
        preparePolicies(
                List.of(typeOption(10L, false, 3)),
                List.of(optionCode(10L, "ELEVATOR", true))
        );
        prepareActiveOptions(option(901L, 10L, "true", 3));

        assertThat(prepareSync(List.of(command("ELEVATOR", "false")))
                .changesRequired()).isTrue();
    }

    @Test
    void prepareSync_changedDisplayOrder_requiresChanges() {
        preparePolicies(
                List.of(typeOption(10L, false, 8)),
                List.of(optionCode(10L, "ELEVATOR", true))
        );
        prepareActiveOptions(option(901L, 10L, "true", 3));

        assertThat(prepareSync(List.of(command("ELEVATOR", "true")))
                .changesRequired()).isTrue();
    }

    @Test
    void prepareSync_missingExistingOption_requiresChanges() {
        preparePolicies(
                List.of(typeOption(10L, false, 1)),
                List.of(optionCode(10L, "ELEVATOR", true))
        );
        prepareActiveOptions(
                option(901L, 10L, "true", 1),
                option(902L, 20L, "false", 2)
        );

        assertThat(prepareSync(List.of(command("ELEVATOR", "true")))
                .changesRequired()).isTrue();
    }

    @Test
    void prepareSync_sameFinalState_doesNotRequireChanges() {
        preparePolicies(
                List.of(
                        typeOption(10L, false, 1),
                        typeOption(20L, false, 2)
                ),
                List.of(
                        optionCode(10L, "ELEVATOR", true),
                        optionCode(20L, "PARKING", true)
                )
        );
        prepareActiveOptions(
                option(901L, 10L, "true", 1),
                option(902L, 20L, "false", 2)
        );

        assertThat(prepareSync(List.of(
                command("ELEVATOR", "true"),
                command("PARKING", "false")
        )).changesRequired()).isFalse();
    }

    @Test
    void prepareSync_emptyRequestWithActiveOption_requiresChanges() {
        preparePolicies(List.of(), List.of());
        prepareActiveOptions(option(901L, 10L, "true", 1));

        assertThat(prepareSync(List.of()).changesRequired()).isTrue();
    }

    @Test
    void prepareSync_emptyRequestWithoutActiveOption_doesNotRequireChanges() {
        preparePolicies(List.of(), List.of());

        assertThat(prepareSync(List.of()).changesRequired()).isFalse();
    }

    @Test
    void prepareSync_missingRequiredOption_fails() {
        preparePolicies(
                List.of(typeOption(10L, true, 1)),
                List.of(optionCode(10L, "ELEVATOR", true))
        );

        assertThatThrownBy(() -> prepareSync(List.of()))
                .isInstanceOf(OptionValueRequiredException.class);
    }

    @Test
    void prepareSync_duplicateRequestCode_fails() {
        assertThatThrownBy(() -> prepareSync(List.of(
                command("ELEVATOR", "true"),
                command("ELEVATOR", "false")
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.DUPLICATED_RESOURCE)
        );
    }

    @Test
    void prepareSync_duplicateActiveTypePolicy_fails() {
        preparePolicies(
                List.of(
                        typeOption(10L, false, 1),
                        typeOption(10L, true, 2)
                ),
                List.of(optionCode(10L, "ELEVATOR", true))
        );

        assertThatThrownBy(() -> prepareSync(List.of(command("ELEVATOR", "true"))))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.SYSTEM_ERROR)
                );
    }

    @Test
    void prepareSync_duplicateActivePropertyOption_fails() {
        preparePolicies(
                List.of(typeOption(10L, false, 1)),
                List.of(optionCode(10L, "ELEVATOR", true))
        );
        prepareActiveOptions(
                option(901L, 10L, "true", 1),
                option(902L, 10L, "false", 1)
        );

        assertThatThrownBy(() -> prepareSync(List.of(command("ELEVATOR", "true"))))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.DUPLICATED_RESOURCE)
                );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "TRUE", "False", "1"})
    void prepareSync_invalidOptionValue_fails(String optionValue) {
        assertThatThrownBy(() -> prepareSync(List.of(
                command("ELEVATOR", optionValue)
        ))).isInstanceOfAny(
                OptionValueRequiredException.class,
                OptionValueInvalidException.class
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void prepareSync_nullOrBlankOptionCode_fails(String optionCode) {
        assertThatThrownBy(() -> prepareSync(List.of(command(optionCode, "true"))))
                .isInstanceOf(OptionCodeNotFoundException.class);
    }

    @Test
    void prepareSync_unknownOrInactiveOptionCode_fails() {
        preparePolicies(
                List.of(typeOption(10L, false, 1)),
                List.of()
        );

        assertThatThrownBy(() -> prepareSync(List.of(command("ELEVATOR", "true"))))
                .isInstanceOf(OptionCodeNotFoundException.class);
    }

    @Test
    void prepareSync_optionNotAllowedForPropertyType_fails() {
        preparePolicies(
                List.of(),
                List.of(optionCode(10L, "ELEVATOR", true))
        );

        assertThatThrownBy(() -> prepareSync(List.of(command("ELEVATOR", "true"))))
                .isInstanceOf(OptionNotAllowedForPropertyTypeException.class);
    }

    @Test
    void prepareSync_registrationDisabledOption_fails() {
        preparePolicies(
                List.of(typeOption(10L, false, 1)),
                List.of(optionCode(10L, "ELEVATOR", false))
        );

        assertThatThrownBy(() -> prepareSync(List.of(command("ELEVATOR", "true"))))
                .isInstanceOf(OptionNotAllowedForPropertyTypeException.class);
    }

    @Test
    void prepareSync_nullPropertyId_fails() {
        assertThatThrownBy(() -> service.prepareSync(
                null,
                PROPERTY_TYPE,
                List.of()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void prepareSync_nullPropertyType_fails() {
        assertThatThrownBy(() -> service.prepareSync(
                PROPERTY_ID,
                null,
                List.of()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void prepareSync_nullCommands_fails() {
        assertThatThrownBy(() -> service.prepareSync(
                PROPERTY_ID,
                PROPERTY_TYPE,
                null
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void prepareSync_neverWritesOptionsOrHistories() {
        preparePolicies(
                List.of(typeOption(10L, false, 2)),
                List.of(optionCode(10L, "ELEVATOR", true))
        );
        prepareActiveOptions(option(901L, 10L, "false", 1));

        prepareSync(List.of(command("ELEVATOR", "true")));

        verifyNoPersistenceInteractions();
        verify(propertyOptionRepository, never()).save(any(PropertyOption.class));
        verify(propertyOptionRepository, never()).saveAll(anyList());
        verify(historyRepository, never()).save(any(PropertyOptionHistory.class));
        verify(historyRepository, never()).saveAll(anyList());
    }

    @Test
    void synchronizeOptions_newOption_createsOptionAndCreateHistory() {
        PropertyOptionCode code = optionCode(10L, "ELEVATOR", true);
        PropertyTypeOption policy = typeOption(10L, false, 3);
        preparePolicies(List.of(policy), List.of(code));

        synchronize(List.of(command("ELEVATOR", "true")));

        PropertyOption created = captureCreatedOption();
        assertThat(created.getPropertyId()).isEqualTo(PROPERTY_ID);
        assertThat(created.getOptionCodeId()).isEqualTo(10L);
        assertThat(created.getOptionValue()).isEqualTo("true");
        assertThat(created.getDisplayOrder()).isEqualTo(3);

        PropertyOptionHistory history = captureHistories().getFirst();
        assertCreateHistory(history, created, "true", 3);
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true"})
    void synchronizeOptions_changedValue_updatesValueAndHistory(
            String beforeValue,
            String afterValue
    ) {
        PropertyOptionCode code = optionCode(10L, "ELEVATOR", true);
        preparePolicies(
                List.of(typeOption(10L, false, 3)),
                List.of(code)
        );
        PropertyOption existing = option(901L, 10L, beforeValue, 3);
        prepareActiveOptions(existing);

        synchronize(List.of(command("ELEVATOR", afterValue)));

        assertThat(existing.getOptionValue()).isEqualTo(afterValue);
        assertThat(existing.getDisplayOrder()).isEqualTo(3);
        PropertyOptionHistory history = captureHistories().getFirst();
        assertUpdateHistory(history, existing, beforeValue, afterValue, 3, 3);
    }

    @Test
    void synchronizeOptions_changedDisplayOrder_updatesOnceAndHistory() {
        PropertyOptionCode code = optionCode(10L, "ELEVATOR", true);
        preparePolicies(
                List.of(typeOption(10L, false, 8)),
                List.of(code)
        );
        PropertyOption existing = option(901L, 10L, "true", 2);
        prepareActiveOptions(existing);

        synchronize(List.of(command("ELEVATOR", "true")));

        assertThat(existing.getOptionValue()).isEqualTo("true");
        assertThat(existing.getDisplayOrder()).isEqualTo(8);
        PropertyOptionHistory history = captureHistories().getFirst();
        assertUpdateHistory(history, existing, "true", "true", 2, 8);
        verify(historyRepository).saveAll(anyList());
    }

    @Test
    void synchronizeOptions_missingExistingOption_softDeletesAndCreatesHistory() {
        preparePolicies(List.of(), List.of());
        PropertyOption existing = option(901L, 10L, "false", 4);
        prepareActiveOptions(existing);

        synchronize(List.of());

        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getDeleteReason()).isEqualTo(REMOVED_REASON);
        PropertyOptionHistory history = captureHistories().getFirst();
        assertSoftDeleteHistory(history, existing, "false", 4);
    }

    @Test
    void synchronizeOptions_sameValueAndDisplayOrder_doesNothing() {
        PropertyOptionCode code = optionCode(10L, "ELEVATOR", true);
        preparePolicies(
                List.of(typeOption(10L, false, 3)),
                List.of(code)
        );
        prepareActiveOptions(option(901L, 10L, "true", 3));

        synchronize(List.of(command("ELEVATOR", "true")));

        verify(propertyOptionRepository, never()).save(any(PropertyOption.class));
        verify(propertyOptionRepository, never()).saveAll(anyList());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void synchronizeOptions_mixedChanges_appliesCreateUpdateDeleteAndNoOp() {
        PropertyOptionCode unchangedCode = optionCode(10L, "ELEVATOR", true);
        PropertyOptionCode createdCode = optionCode(20L, "PARKING", true);
        PropertyOptionCode updatedCode = optionCode(30L, "PET", true);
        preparePolicies(
                List.of(
                        typeOption(10L, false, 1),
                        typeOption(20L, false, 2),
                        typeOption(30L, false, 7)
                ),
                List.of(unchangedCode, createdCode, updatedCode)
        );
        PropertyOption unchanged = option(901L, 10L, "true", 1);
        PropertyOption updated = option(902L, 30L, "true", 3);
        PropertyOption removed = option(903L, 40L, "false", 4);
        prepareActiveOptions(unchanged, updated, removed);

        synchronize(List.of(
                command("ELEVATOR", "true"),
                command("PARKING", "false"),
                command("PET", "false")
        ));

        PropertyOption created = captureCreatedOption();
        assertThat(created.getOptionCodeId()).isEqualTo(20L);
        assertThat(updated.getOptionValue()).isEqualTo("false");
        assertThat(updated.getDisplayOrder()).isEqualTo(7);
        assertThat(removed.getDeletedAt()).isNotNull();
        assertThat(unchanged.getDeletedAt()).isNull();

        List<PropertyOptionHistory> histories = captureHistories();
        assertThat(histories).hasSize(3);
        assertThat(histories)
                .extracting(PropertyOptionHistory::getChangeType)
                .containsExactlyInAnyOrder(
                        OptionChangeType.CREATE,
                        OptionChangeType.UPDATE,
                        OptionChangeType.SOFT_DELETE
                );
        assertCreateHistory(
                historyOf(histories, OptionChangeType.CREATE),
                created,
                "false",
                2
        );
        assertUpdateHistory(
                historyOf(histories, OptionChangeType.UPDATE),
                updated,
                "true",
                "false",
                3,
                7
        );
        assertSoftDeleteHistory(
                historyOf(histories, OptionChangeType.SOFT_DELETE),
                removed,
                "false",
                4
        );
    }

    @Test
    void synchronizeOptions_missingRequiredOption_failsBeforeChanges() {
        PropertyOptionCode requiredCode = optionCode(10L, "ELEVATOR", true);
        preparePolicies(
                List.of(typeOption(10L, true, 1)),
                List.of(requiredCode)
        );
        PropertyOption existing = option(901L, 20L, "true", 2);
        prepareActiveOptions(existing);

        assertThatThrownBy(() -> synchronize(List.of()))
                .isInstanceOf(OptionValueRequiredException.class);

        assertThat(existing.getDeletedAt()).isNull();
        verifyNoPersistenceInteractions();
        verify(queryRepository, never()).findActiveOptionsByPropertyId(PROPERTY_ID);
    }

    @Test
    void synchronizeOptions_duplicateRequestCode_failsBeforeChanges() {
        assertThatThrownBy(() -> synchronize(List.of(
                command("ELEVATOR", "true"),
                command("ELEVATOR", "false")
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.DUPLICATED_RESOURCE)
        );

        verifyNoPersistenceInteractions();
        verify(queryRepository, never()).findActiveTypeOptions(PROPERTY_TYPE);
    }

    @Test
    void synchronizeOptions_duplicateActiveTypePolicy_failsBeforeChanges() {
        preparePolicies(
                List.of(
                        typeOption(10L, false, 1),
                        typeOption(10L, true, 9)
                ),
                List.of(optionCode(10L, "ELEVATOR", true))
        );

        assertThatThrownBy(() -> synchronize(List.of(command("ELEVATOR", "true"))))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.SYSTEM_ERROR)
                );

        verifyNoPersistenceInteractions();
        verify(queryRepository, never()).findActiveOptionsByPropertyId(PROPERTY_ID);
    }

    @Test
    void synchronizeOptions_duplicateActivePropertyOption_failsBeforeChanges() {
        preparePolicies(
                List.of(typeOption(10L, false, 1)),
                List.of(optionCode(10L, "ELEVATOR", true))
        );
        prepareActiveOptions(
                option(901L, 10L, "true", 1),
                option(902L, 10L, "false", 1)
        );

        assertThatThrownBy(() -> synchronize(List.of(command("ELEVATOR", "true"))))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.DUPLICATED_RESOURCE)
                );

        verifyNoPersistenceInteractions();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "TRUE", "False", "1"})
    void synchronizeOptions_invalidOptionValue_fails(String optionValue) {
        assertThatThrownBy(() -> synchronize(List.of(
                command("ELEVATOR", optionValue)
        ))).isInstanceOfAny(
                OptionValueRequiredException.class,
                OptionValueInvalidException.class
        );

        verifyNoPersistenceInteractions();
        verify(queryRepository, never()).findActiveTypeOptions(PROPERTY_TYPE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void synchronizeOptions_exactLowercaseBooleanValue_isAccepted(String optionValue) {
        preparePolicies(
                List.of(typeOption(10L, false, 1)),
                List.of(optionCode(10L, "ELEVATOR", true))
        );

        synchronize(List.of(command("ELEVATOR", optionValue)));

        assertThat(captureCreatedOption().getOptionValue()).isEqualTo(optionValue);
    }

    @Test
    void synchronizeOptions_unknownOptionCode_fails() {
        preparePolicies(List.of(), List.of());

        assertThatThrownBy(() -> synchronize(List.of(command("UNKNOWN", "true"))))
                .isInstanceOf(OptionCodeNotFoundException.class);

        verifyNoPersistenceInteractions();
    }

    @Test
    void synchronizeOptions_inactiveOptionCode_fails() {
        preparePolicies(
                List.of(typeOption(10L, false, 1)),
                List.of()
        );

        assertThatThrownBy(() -> synchronize(List.of(command("ELEVATOR", "true"))))
                .isInstanceOf(OptionCodeNotFoundException.class);

        verifyNoPersistenceInteractions();
    }

    @Test
    void synchronizeOptions_optionNotAllowedForPropertyType_fails() {
        preparePolicies(
                List.of(),
                List.of(optionCode(10L, "ELEVATOR", true))
        );

        assertThatThrownBy(() -> synchronize(List.of(command("ELEVATOR", "true"))))
                .isInstanceOf(OptionNotAllowedForPropertyTypeException.class);

        verifyNoPersistenceInteractions();
    }

    @Test
    void synchronizeOptions_registrationDisabledOption_fails() {
        preparePolicies(
                List.of(typeOption(10L, false, 1)),
                List.of(optionCode(10L, "ELEVATOR", false))
        );

        assertThatThrownBy(() -> synchronize(List.of(command("ELEVATOR", "true"))))
                .isInstanceOf(OptionNotAllowedForPropertyTypeException.class);

        verifyNoPersistenceInteractions();
    }

    @Test
    void synchronizeOptions_softDeletedExistingOption_isCreatedAgain() {
        PropertyOption softDeleted = option(901L, 10L, "false", 1);
        softDeleted.softDelete(
                ACTOR_CONTEXT,
                Instant.now().minusSeconds(10),
                "OLD_DELETE"
        );
        preparePolicies(
                List.of(typeOption(10L, false, 5)),
                List.of(optionCode(10L, "ELEVATOR", true))
        );
        when(queryRepository.findActiveOptionsByPropertyId(PROPERTY_ID))
                .thenReturn(List.of());

        synchronize(List.of(command("ELEVATOR", "true")));

        PropertyOption created = captureCreatedOption();
        assertThat(created.getPropertyOptionId())
                .isNotEqualTo(softDeleted.getPropertyOptionId());
        assertThat(created.getOptionCodeId()).isEqualTo(10L);
        assertCreateHistory(captureHistories().getFirst(), created, "true", 5);
    }

    private void synchronize(List<PropertyOptionCreateCommand> commands) {
        service.synchronizeOptions(
                PROPERTY_ID,
                REVISION_ID,
                PROPERTY_TYPE,
                commands,
                CHANGED_FIELDS,
                ACTOR_CONTEXT
        );
    }

    private PropertyOptionCommandService.OptionSyncPlan prepareSync(
            List<PropertyOptionCreateCommand> commands
    ) {
        return service.prepareSync(
                PROPERTY_ID,
                PROPERTY_TYPE,
                commands
        );
    }

    private void preparePolicies(
            List<PropertyTypeOption> policies,
            List<PropertyOptionCode> activeCodes
    ) {
        when(queryRepository.findActiveTypeOptions(PROPERTY_TYPE))
                .thenReturn(policies);
        when(queryRepository.findActiveOptionCodesByCodes(anyCollection()))
                .thenAnswer(invocation -> {
                    Collection<String> requestedCodes = invocation.getArgument(0);
                    return activeCodes.stream()
                            .filter(code -> requestedCodes.contains(code.getOptionCode()))
                            .toList();
                });
        when(queryRepository.findActiveOptionCodesByIds(anyCollection()))
                .thenAnswer(invocation -> {
                    Collection<Long> requestedIds = invocation.getArgument(0);
                    return activeCodes.stream()
                            .filter(code -> requestedIds.contains(code.getOptionCodeId()))
                            .toList();
                });
    }

    private void prepareActiveOptions(PropertyOption... options) {
        when(queryRepository.findActiveOptionsByPropertyId(PROPERTY_ID))
                .thenReturn(List.of(options));
    }

    private PropertyOptionCreateCommand command(String code, String value) {
        return new PropertyOptionCreateCommand(code, value);
    }

    private PropertyOptionCode optionCode(
            Long optionCodeId,
            String code,
            boolean registrationEnabled
    ) {
        PropertyOptionCode optionCode = mock(PropertyOptionCode.class);
        when(optionCode.getOptionCodeId()).thenReturn(optionCodeId);
        when(optionCode.getOptionCode()).thenReturn(code);
        when(optionCode.isRegistrationEnabled()).thenReturn(registrationEnabled);
        return optionCode;
    }

    private PropertyTypeOption typeOption(
            Long optionCodeId,
            boolean required,
            int displayOrder
    ) {
        PropertyTypeOption typeOption = mock(PropertyTypeOption.class);
        when(typeOption.getOptionCodeId()).thenReturn(optionCodeId);
        when(typeOption.isRequired()).thenReturn(required);
        when(typeOption.getDisplayOrder()).thenReturn(displayOrder);
        return typeOption;
    }

    private PropertyOption option(
            Long propertyOptionId,
            Long optionCodeId,
            String value,
            int displayOrder
    ) {
        PropertyOption option = new PropertyOption(
                PROPERTY_ID,
                optionCodeId,
                value,
                displayOrder,
                ACTOR_CONTEXT
        );
        ReflectionTestUtils.setField(
                option,
                "propertyOptionId",
                propertyOptionId
        );
        return option;
    }

    private PropertyOption captureCreatedOption() {
        ArgumentCaptor<PropertyOption> captor =
                ArgumentCaptor.forClass(PropertyOption.class);
        verify(propertyOptionRepository).save(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<PropertyOptionHistory> captureHistories() {
        ArgumentCaptor<List<PropertyOptionHistory>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(historyRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private PropertyOptionHistory historyOf(
            List<PropertyOptionHistory> histories,
            OptionChangeType changeType
    ) {
        return histories.stream()
                .filter(history -> history.getChangeType() == changeType)
                .findFirst()
                .orElseThrow();
    }

    private void assertCreateHistory(
            PropertyOptionHistory history,
            PropertyOption option,
            String afterValue,
            int afterDisplayOrder
    ) {
        assertCommonHistory(history, option, OptionChangeType.CREATE);
        assertThat(history.getBeforeValue()).isNull();
        assertThat(history.getAfterValue()).isEqualTo(afterValue);
        assertThat(history.getBeforeDisplayOrder()).isNull();
        assertThat(history.getAfterDisplayOrder()).isEqualTo(afterDisplayOrder);
        assertThat(history.getBeforeDeletedAt()).isNull();
        assertThat(history.getAfterDeletedAt()).isNull();
    }

    private void assertUpdateHistory(
            PropertyOptionHistory history,
            PropertyOption option,
            String beforeValue,
            String afterValue,
            int beforeDisplayOrder,
            int afterDisplayOrder
    ) {
        assertCommonHistory(history, option, OptionChangeType.UPDATE);
        assertThat(history.getBeforeValue()).isEqualTo(beforeValue);
        assertThat(history.getAfterValue()).isEqualTo(afterValue);
        assertThat(history.getBeforeDisplayOrder()).isEqualTo(beforeDisplayOrder);
        assertThat(history.getAfterDisplayOrder()).isEqualTo(afterDisplayOrder);
        assertThat(history.getBeforeDeletedAt()).isNull();
        assertThat(history.getAfterDeletedAt()).isNull();
    }

    private void assertSoftDeleteHistory(
            PropertyOptionHistory history,
            PropertyOption option,
            String value,
            int displayOrder
    ) {
        assertCommonHistory(history, option, OptionChangeType.SOFT_DELETE);
        assertThat(history.getBeforeValue()).isEqualTo(value);
        assertThat(history.getAfterValue()).isEqualTo(value);
        assertThat(history.getBeforeDisplayOrder()).isEqualTo(displayOrder);
        assertThat(history.getAfterDisplayOrder()).isEqualTo(displayOrder);
        assertThat(history.getBeforeDeletedAt()).isNull();
        assertThat(history.getAfterDeletedAt()).isEqualTo(option.getDeletedAt());
    }

    private void assertCommonHistory(
            PropertyOptionHistory history,
            PropertyOption option,
            OptionChangeType changeType
    ) {
        assertThat(history.getPropertyRevisionId()).isEqualTo(REVISION_ID);
        assertThat(history.getPropertyOptionId()).isEqualTo(option.getPropertyOptionId());
        assertThat(history.getOptionCodeId()).isEqualTo(option.getOptionCodeId());
        assertThat(history.getChangedFields()).isEqualTo(CHANGED_FIELDS);
        assertThat(history.getChangeType()).isEqualTo(changeType);
    }

    private void verifyNoPersistenceInteractions() {
        verifyNoInteractions(propertyOptionRepository, historyRepository);
    }
}
