package com.zipdaprojecttak.global.context;

import com.zipdaprojecttak.global.context.constant.ActorRole;
import com.zipdaprojecttak.global.context.constant.InternalHeaderName;
import com.zipdaprojecttak.global.error.custom.business.UnauthenticatedException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Locale;

@Component
public class ActorContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ActorContext.class.isAssignableFrom(
                parameter.getParameterType()
        );
    }

    @Override
    public ActorContext resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer modelAndViewContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory webDataBinderFactory
    ) {
        String userIdHeader = webRequest.getHeader(
                InternalHeaderName.X_USER_ID
        );

        String userRoleHeader = webRequest.getHeader(
                InternalHeaderName.X_USER_ROLE
        );

        Long memberId = parseMemberId(userIdHeader);
        ActorRole actorRole = parseActorRole(userRoleHeader);
        String traceId = resolveTraceId(webRequest);

        return ActorContext.member(
                memberId,
                actorRole,
                traceId
        );
    }

    private Long parseMemberId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new UnauthenticatedException(
                    "X-User-Id 헤더가 없습니다."
            );
        }

        final long memberId;

        try {
            memberId = Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException exception) {
            throw new UnauthenticatedException(
                    "X-User-Id 헤더 형식이 올바르지 않습니다."
            );
        }

        if (memberId <= 0) {
            throw new UnauthenticatedException(
                    "X-User-Id는 0보다 커야 합니다."
            );
        }

        return memberId;
    }

    private ActorRole parseActorRole(String userRoleHeader) {
        if (userRoleHeader == null || userRoleHeader.isBlank()) {
            throw new UnauthenticatedException(
                    "X-User-Role 헤더가 없습니다."
            );
        }

        try {
            return ActorRole.valueOf(
                    userRoleHeader
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new UnauthenticatedException(
                    "X-User-Role 헤더 값이 올바르지 않습니다."
            );
        }
    }

    private String resolveTraceId(NativeWebRequest webRequest) {
        Object traceIdAttribute = webRequest.getAttribute(
                TraceIdContext.REQUEST_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST
        );

        if (traceIdAttribute instanceof String traceId
                && !traceId.isBlank()) {
            return traceId;
        }

        return TraceIdContext.getOrCreate();
    }
}