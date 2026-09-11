package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.global.id.TsidString;
import io.swagger.v3.oas.annotations.media.Schema;

public record PropertyMapBoundsItemResponse(

        @Schema(
                description = "매물 ID",
                example = "884685586571263701"
        )
        @TsidString
        Long propertyId,

        @Schema(
                description = "매물의 Region ID",
                example = "123"
        )
        Long regionId,

        @Schema(
                description = "매물 제목",
                example = "역세권 채광 좋은 아파트"
        )
        String title,

        @Schema(
                description = "매물 유형",
                example = "APARTMENT"
        )
        PropertyType propertyType,

        @Schema(
                description = "거래 유형",
                example = "JEONSE"
        )
        TransactionType transactionType,

        @Schema(
                description = "매매 가격",
                nullable = true,
                example = "850000000"
        )
        Long salePrice,

        @Schema(
                description = "보증금",
                nullable = true,
                example = "500000000"
        )
        Long deposit,

        @Schema(
                description = "월세",
                nullable = true,
                example = "1200000"
        )
        Long monthlyRent,

        @Schema(
                description = "공개 가능한 주소",
                example = "서울특별시 강남구 역삼동"
        )
        String publicAddress,

        @Schema(
                description = "비식별 공개 좌표의 위도",
                example = "37.4979"
        )
        double latitude,

        @Schema(
                description = "비식별 공개 좌표의 경도",
                example = "127.0276"
        )
        double longitude

) {
}
