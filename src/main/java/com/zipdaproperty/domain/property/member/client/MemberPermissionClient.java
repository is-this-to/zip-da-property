package com.zipdaproperty.domain.property.member.client;

import com.zipdaproperty.domain.property.member.client.response.MemberPermissionApiResponse;
import com.zipdaproperty.domain.property.member.constant.MemberPermissionAction;
import com.zipdaproperty.global.context.TraceIdContext;
import com.zipdaproperty.global.context.constant.InternalHeaderName;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class MemberPermissionClient {

    private final RestClient restClient;

    public MemberPermissionClient(
            @Qualifier("memberRestClient")
            RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public MemberPermissionResult getPermission(
            Long memberId,
            MemberPermissionAction action
    ) {
        try {
            MemberPermissionApiResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/members/{memberId}/permissions")
                            .queryParam("action", action.name())
                            .build(memberId)
                    )
                    .header(
                            InternalHeaderName.X_TRACE_ID,
                            TraceIdContext.getOrCreate()
                    )
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, errorResponse) -> {
                                log.warn(
                                        "Member permission API 호출 실패. status={}, action={}",
                                        errorResponse.getStatusCode(),
                                        action
                                );
                                throw unavailable();
                            }
                    )
                    .body(MemberPermissionApiResponse.class);

            if (response == null
                    || !"00".equals(response.code())
                    || response.data() == null
                    || response.data().allowed() == null) {
                throw unavailable();
            }

            return new MemberPermissionResult(
                    response.data().allowed(),
                    response.data().reason()
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn(
                    "Member permission API 통신 오류. action={}, exception={}",
                    action,
                    exception.getClass().getSimpleName()
            );
            throw unavailable();
        }
    }

    private BusinessException unavailable() {
        return new BusinessException(
                CustomResponseCode.MEMBER_API_UNAVAILABLE,
                "Member 권한 확인 API를 사용할 수 없습니다."
        );
    }
}
