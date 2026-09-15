package com.zipdaproperty.domain.property.constant;

public enum RevisionChangeType {

    CREATE, // 매물 최초 생성
    UPDATE, //기존 매물 정보 변경
    SOFT_DELETE,
    RESTORE  //소프트 삭제된 매물을 다시 복구한 경우
}