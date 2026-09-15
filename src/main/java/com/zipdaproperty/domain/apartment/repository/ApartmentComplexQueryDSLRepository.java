package com.zipdaproperty.domain.apartment.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.apartment.response.ApartmentComplexSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.zipdaproperty.domain.apartment.entity.QApartmentComplex.apartmentComplex;

@Repository
@RequiredArgsConstructor
public class ApartmentComplexQueryDSLRepository {

    private final JPAQueryFactory queryFactory;

    public List<ApartmentComplexSummaryResponse> findActiveComplexes(
            Long regionId,
            String keyword,
            int limit
    ) {
        return queryFactory
                .select(Projections.constructor(
                        ApartmentComplexSummaryResponse.class,
                        apartmentComplex.apartmentComplexId,
                        apartmentComplex.complexName,
                        apartmentComplex.roadAddress,
                        apartmentComplex.jibunAddress,
                        apartmentComplex.totalBuildings,
                        apartmentComplex.totalHouseholds,
                        apartmentComplex.totalParkingSpaces,
                        apartmentComplex.approvalDate
                ))
                .from(apartmentComplex)
                .where(
                        apartmentComplex.regionId.eq(regionId),
                        apartmentComplex.isActive.isTrue(),
                        apartmentComplex.deletedAt.isNull(),
                        keywordContains(keyword)
                )
                .orderBy(
                        apartmentComplex.complexName.asc(),
                        apartmentComplex.apartmentComplexId.asc()
                )
                .limit(limit)
                .fetch();
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null) {
            return null;
        }

        return apartmentComplex.complexName.containsIgnoreCase(keyword)
                .or(apartmentComplex.roadAddress.containsIgnoreCase(keyword))
                .or(apartmentComplex.jibunAddress.containsIgnoreCase(keyword));
    }
}
