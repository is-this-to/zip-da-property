package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.report.repository.PropertyReportMyListQueryRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportMyListQueryRow;
import com.zipdaproperty.domain.report.request.PropertyReportMyListRequest;
import com.zipdaproperty.domain.report.response.PropertyReportMyListItemResponse;
import com.zipdaproperty.domain.report.response.PropertyReportMyListResponse;
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
public class PropertyReportMyListService {

    private final PropertyReportMyListQueryRepository propertyReportMyListQueryRepository;

    @Transactional(readOnly = true)
    public PropertyReportMyListResponse findMyReports(
            PropertyReportMyListRequest request,
            ActorContext actorContext
    ) {
        validateActor(actorContext);

        PropertyReportMyListCursor cursor = PropertyReportMyListCursor.decode(request.cursor());
        int size = request.size();
        List<PropertyReportMyListQueryRow> rows = propertyReportMyListQueryRepository.findMyReports(
                actorContext.memberId(), cursor.createdAt(), cursor.reportId(), size + 1
        );

        boolean hasNext = rows.size() > size;
        List<PropertyReportMyListQueryRow> visibleRows = rows.stream().limit(size).toList();
        List<PropertyReportMyListItemResponse> items = visibleRows.stream()
                .map(this::toItemResponse)
                .toList();

        return new PropertyReportMyListResponse(
                items,
                createNextCursor(visibleRows, hasNext),
                hasNext
        );
    }

    private void validateActor(ActorContext actorContext) {
        if (actorContext == null || !actorContext.isMemberRequest()) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "회원 요청만 내 신고 목록을 조회할 수 있습니다."
            );
        }

        ActorRole role = actorContext.role();
        if (role != ActorRole.USER && role != ActorRole.AGENT) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "USER 또는 AGENT만 내 신고 목록을 조회할 수 있습니다."
            );
        }
    }

    private String createNextCursor(
            List<PropertyReportMyListQueryRow> visibleRows,
            boolean hasNext
    ) {
        if (!hasNext) {
            return null;
        }

        PropertyReportMyListQueryRow lastRow = visibleRows.get(visibleRows.size() - 1);
        return PropertyReportMyListCursor.from(lastRow.createdAt(), lastRow.reportId()).encode();
    }

    private PropertyReportMyListItemResponse toItemResponse(PropertyReportMyListQueryRow row) {
        return new PropertyReportMyListItemResponse(
                row.reportId(),
                row.propertyId(),
                row.reasonCode(),
                row.status(),
                row.createdAt()
        );
    }
}
