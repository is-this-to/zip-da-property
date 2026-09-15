package com.zipdaproperty.domain.region.constant;

public enum RegionType {
    // 주소 검색에서 기본 3계층까지만 표시
    SIDO,           // Level 1: 시·도
    SIGUNGU,        // Level 2: 시·군·구
    EUPMYEONDONG,   // Level 3: 읍·면·동
    RI,             // Level 4: 리, 내부 기준정보
}
