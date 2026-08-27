package com.zipdaprojecttak.global.context;


import com.zipdaprojecttak.global.context.constant.ActionSource;
import com.zipdaprojecttak.global.context.constant.ActorRole;

import java.util.Objects;

public record ActorContext(
        Long memberId,
        ActorRole role,
        ActionSource actionSource,
        String traceId
) {

    public ActorContext {
        Objects.requireNonNull(
                actionSource,
                "actionSource는 필수입니다."
        );

        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException(
                    "traceId는 비어 있을 수 없습니다."
            );
        }

        if (actionSource == ActionSource.MEMBER) {
            if (memberId == null || memberId <= 0) {
                throw new IllegalArgumentException(
                        "회원 요청에는 유효한 memberId가 필요합니다."
                );
            }

            Objects.requireNonNull(
                    role,
                    "회원 요청에는 role이 필요합니다."
            );
        }
    }

    public static ActorContext member(
            Long memberId,
            ActorRole role,
            String traceId
    ) {
        return new ActorContext(
                memberId,
                role,
                ActionSource.MEMBER,
                traceId
        );
    }

    public static ActorContext system(String traceId) {
        return new ActorContext(
                null,
                null,
                ActionSource.SYSTEM,
                traceId
        );
    }

    public static ActorContext batch(String traceId) {
        return new ActorContext(
                null,
                null,
                ActionSource.BATCH,
                traceId
        );
    }

    public boolean isMemberRequest() {
        return actionSource == ActionSource.MEMBER;
    }
}