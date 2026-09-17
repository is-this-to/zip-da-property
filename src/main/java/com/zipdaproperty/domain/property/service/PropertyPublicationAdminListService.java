package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.repository.PropertyPublicationAdminListQueryRepository;
import com.zipdaproperty.domain.property.request.PropertyPublicationAdminListRequest;
import com.zipdaproperty.domain.property.response.PropertyPublicationAdminListItemResponse;
import com.zipdaproperty.domain.property.response.PropertyPublicationAdminListResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PropertyPublicationAdminListService {
    private static final ZoneId REGISTRATION_ZONE = ZoneId.of("Asia/Seoul");

    private final PropertyPublicationAdminListQueryRepository repository;
    private final PropertyAdminReviewAccess access;

    @Transactional(readOnly = true)
    public PropertyPublicationAdminListResponse find(PropertyPublicationAdminListRequest request, ActorContext actor) {
        access.requireAdmin(actor);
        validateRegisteredPeriod(request.registeredFrom(), request.registeredTo());

        // 페이지 조건
        PageRequest pageable = PageRequest.of(
            request.page(),
            request.size(),
            Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("propertyId"))
        );

        // 등록 기간
        Instant registeredFrom = startOfDay(request.registeredFrom());
        Instant registeredToExclusive = startOfDay(nextDay(request.registeredTo()));

        Page<PropertyPublicationAdminListItemResponse> result = repository.find(
            request.status(), request.verificationStatus(), request.publisherType(),
            request.propertyType(), request.address(), registeredFrom, registeredToExclusive,
            pageable);

        // 페이지 응답
        return new PropertyPublicationAdminListResponse(
            result.getContent(), result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages());
    }

    private void validateRegisteredPeriod(LocalDate registeredFrom, LocalDate registeredTo) {
        if (registeredFrom != null && registeredTo != null && registeredFrom.isAfter(registeredTo)) {
            throw new BusinessException(
                CustomResponseCode.INVALID_REQUEST,
                "등록기간 시작일은 종료일보다 늦을 수 없습니다."
            );
        }
    }

    private Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(REGISTRATION_ZONE).toInstant();
    }

    private LocalDate nextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1);
    }
}
