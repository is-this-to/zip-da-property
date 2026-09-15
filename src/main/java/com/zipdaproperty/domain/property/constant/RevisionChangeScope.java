package com.zipdaproperty.domain.property.constant;

public enum RevisionChangeScope {

    PROPERTY,  // Property 테이블의 핵심 정보가 변경된 경우 (제목,매매가,전세금 등등)
    OPTION,  // Property 옵션 영역이 변경된 경우
    STATUS, // 매물 상태 변경
    COMPOSITE  // 한번의 요청에서 여러 영역이 한번에 변경된 경우
}