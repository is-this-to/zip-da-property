package com.zipdaproperty.global.context.constant;

public enum ActionSource {
    MEMBER, // 유저가 직접 수정한 정보
    SYSTEM, // 서버 로직이 자동으로 수정
    BATCH // 정기/일괄 작업으로 수정 (여러 데이터
}