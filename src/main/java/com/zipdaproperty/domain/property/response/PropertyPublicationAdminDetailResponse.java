package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.global.id.TsidString;

public record PropertyPublicationAdminDetailResponse(
    @TsidString Long authorMemberId,
    PropertyEditDetailResponse property,
    PropertyPublicationAdminVerificationSummary latestVerification
) {}