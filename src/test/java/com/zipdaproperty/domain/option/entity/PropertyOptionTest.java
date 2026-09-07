package com.zipdaproperty.domain.option.entity;

import com.zipdaproperty.global.context.ActorContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyOptionTest {

    private static final ActorContext CREATE_ACTOR = ActorContext.system("create-trace");
    private static final ActorContext UPDATE_ACTOR = ActorContext.batch("update-trace");

    @Test
    void constructor_validValue_createsActiveOption() {
        PropertyOption option = new PropertyOption(1L, 2L, "false", 3, CREATE_ACTOR);

        assertThat(option.getPropertyId()).isEqualTo(1L);
        assertThat(option.getOptionCodeId()).isEqualTo(2L);
        assertThat(option.getOptionValue()).isEqualTo("false");
        assertThat(option.getDisplayOrder()).isEqualTo(3);
        assertThat(option.isVerified()).isFalse();
        assertThat(option.isDeleted()).isFalse();
    }

    @Test
    void constructor_invalidValue_rejects() {
        assertThatThrownBy(() -> new PropertyOption(1L, 2L, "TRUE", 3, CREATE_ACTOR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeValue_activeOption_changesValueAndAuditActor() {
        PropertyOption option = new PropertyOption(1L, 2L, "true", 3, CREATE_ACTOR);

        option.changeValue("false", UPDATE_ACTOR);

        assertThat(option.getOptionValue()).isEqualTo("false");
        assertThat(option.getUpdatedByMemberId()).isNull();
        assertThat(option.getActionSource()).isEqualTo(UPDATE_ACTOR.actionSource());
    }

    @Test
    void softDelete_activeOption_recordsDeletionWithoutRestore() {
        PropertyOption option = new PropertyOption(1L, 2L, "true", 3, CREATE_ACTOR);
        Instant deletedAt = Instant.parse("2026-09-07T00:00:00Z");

        option.softDelete(UPDATE_ACTOR, deletedAt, "옵션 제거");

        assertThat(option.isDeleted()).isTrue();
        assertThat(option.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(option.getDeleteReason()).isEqualTo("옵션 제거");
    }

    @Test
    void changeValue_deletedOption_rejects() {
        PropertyOption option = new PropertyOption(1L, 2L, "true", 3, CREATE_ACTOR);
        option.softDelete(UPDATE_ACTOR, Instant.parse("2026-09-07T00:00:00Z"), "옵션 제거");

        assertThatThrownBy(() -> option.changeValue("false", UPDATE_ACTOR))
                .isInstanceOf(IllegalStateException.class);
    }
}
