package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.report.repository.PropertyReportAdminListQueryRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportAdminListQueryRow;
import com.zipdaproperty.domain.report.request.PropertyReportAdminListRequest;
import com.zipdaproperty.domain.report.response.PropertyReportAdminListItemResponse;
import com.zipdaproperty.domain.report.response.PropertyReportAdminListResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyReportAdminListService {

    private final PropertyReportAdminListQueryRepository propertyReportAdminListQueryRepository;

    @Transactional(readOnly = true)
    public PropertyReportAdminListResponse findReports(
            PropertyReportAdminListRequest request,
            ActorContext actorContext
    ) {
        validateActor(actorContext);

        PropertyReportAdminListCursor cursor =
                PropertyReportAdminListCursor.decode(request.cursor());
        int size = request.size();
        List<PropertyReportAdminListQueryRow> rows =
                propertyReportAdminListQueryRepository.findReports(
                        cursor.createdAt(), cursor.reportId(), size + 1
                );

        boolean hasNext = rows.size() > size;
        List<PropertyReportAdminListQueryRow> visibleRows = rows.stream()
                .limit(size)
                .toList();
        List<PropertyReportAdminListItemResponse> items = visibleRows.stream()
                .map(this::toItemResponse)
                .toList();

        return new PropertyReportAdminListResponse(
                items,
                createNextCursor(visibleRows, hasNext),
                hasNext
        );
    }

    private void validateActor(ActorContext actorContext) {
        if (actorContext == null || !actorContext.isMemberRequest()) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "관리자만 신고 목록을 조회할 수 있습니다."
            );
        }

        ActorRole role = actorContext.role();
        if (role != ActorRole.CS_ADMIN && role != ActorRole.SUPER_ADMIN) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "CS_ADMIN 또는 SUPER_ADMIN만 신고 목록을 조회할 수 있습니다."
            );
        }
    }

    private String createNextCursor(
            List<PropertyReportAdminListQueryRow> visibleRows,
            boolean hasNext
    ) {
        if (!hasNext) {
            return null;
        }

        PropertyReportAdminListQueryRow lastRow = visibleRows.get(visibleRows.size() - 1);
        return PropertyReportAdminListCursor
                .from(lastRow.createdAt(), lastRow.reportId())
                .encode();
    }

    private PropertyReportAdminListItemResponse toItemResponse(
            PropertyReportAdminListQueryRow row
    ) {
        return new PropertyReportAdminListItemResponse(
                row.reportId(),
                row.propertyId(),
                row.reporterMemberId(),
                row.reasonCode(),
                row.status(),
                row.riskScore(),
                row.assignedAdminId(),
                row.createdAt()
        );
    }
}
