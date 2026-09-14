package com.zipdaproperty.domain.report.client;

public interface MemberSanctionClient {

    void requestSanction(Long memberId, String reason);
}
