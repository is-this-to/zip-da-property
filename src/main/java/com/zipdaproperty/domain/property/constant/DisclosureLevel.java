package com.zipdaproperty.domain.property.constant;

/**
 * 매물 위치를 외부에 어는 수준으로 공개할지 나타낸다.
 */
public enum DisclosureLevel {

    /**
     * 법정동 수준으로만 공개한다.
     */
    DONG,

    /**
     * 정확한 위치에서 일정 거리만큼 이동시킨 근사 위치를 공개한다.
     */
    APPROXIMATE,

    /**
     * 개별 매물 위치 대신 아파트 단지 대표 중심점을 공개한다.
     */
    COMPLEX_CENTER
}
