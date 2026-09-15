package com.zipdaproperty.domain.file.entity;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyFileVerificationLinkTest {
    private final ActorContext actor = ActorContext.member(10L, ActorRole.USER, "test");

    @Test
    void verifiedEvidenceBecomesLinkedWithLinkedAtExactlyOnce() {
        PropertyFile file = PropertyFile.create(1L, "session", FilePurpose.VERIFICATION,
                "proof.pdf", 3L, "key", Instant.now().plusSeconds(60), actor);
        file.complete("checksum", "application/pdf", actor);
        assertThat(file.getUploadStatus()).isEqualTo(UploadStatus.VERIFIED);
        file.markLinked(actor);
        Instant linkedAt = file.getLinkedAt();
        assertThat(file.getUploadStatus()).isEqualTo(UploadStatus.LINKED);
        assertThat(linkedAt).isNotNull();
        assertThat(file.isReadyToLink()).isFalse();
        assertThatThrownBy(() -> file.markLinked(actor)).isInstanceOf(IllegalStateException.class);
        assertThat(file.getLinkedAt()).isEqualTo(linkedAt);
    }
}
