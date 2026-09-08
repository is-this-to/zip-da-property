package com.zipdaproperty.domain.property.constant;

/**
 * 매물 주소와 좌표가 어떤 경로로 생성되었는지 나타낸다.
 */
public enum LocationSource {
    /**
     * 카카오 Local REST API를 이용하여 생성한 위치다.
     */
    KAKAO_LOCAL,

    /**
     * 관리자가 직접 입력하거나 보정한 위치다.
     */
    ADMIN,

    /**
     * 공공데이터 또는 일괄 작업을 통해 생성한 위치다.
     */
    BATCH
}
