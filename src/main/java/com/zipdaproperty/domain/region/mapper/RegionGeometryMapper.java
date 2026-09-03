package com.zipdaproperty.domain.region.mapper;

import com.zipdaproperty.domain.region.constant.RegionGeometryType;
import com.zipdaproperty.domain.region.entity.RegionBoundary;
import com.zipdaproperty.domain.region.response.RegionDetailResponse;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class RegionGeometryMapper {
    public RegionDetailResponse.GeometryResponse toGeoJson(RegionBoundary boundary){

        MultiPolygon multiPolygon = boundary.getBoundaryGeometry();

        /*
         *  DB에는 MultiPolygon으로 저장되지만
         *  원본이 Polygon이고 도형이 하나라면
         *  API에서는 Polygon 구조로 반환
         */
        if(boundary.getGeometryType() == RegionGeometryType.POLYGON && multiPolygon.getNumGeometries() == 1){

            Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);

            return new RegionDetailResponse.GeometryResponse(
                    "Polygon",
                    toPolygonCoordinates(polygon)
            );
        }

        return new RegionDetailResponse.GeometryResponse(
          "MultiPolygon",
                toMultiPolygonCoordinates(multiPolygon)
        );

    }

    private List<List<List<List<Double>>>> toMultiPolygonCoordinates(MultiPolygon multiPolygon){
        List<List<List<List<Double>>>> polygons = new ArrayList<>();

        for(int i = 0; i < multiPolygon.getNumGeometries(); i++){
            Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);

            polygons.add(toPolygonCoordinates(polygon));
        }
        return polygons;
    }

    private List<List<List<Double>>> toPolygonCoordinates(Polygon polygon){
        List<List<List<Double>>> rings = new ArrayList<>();

        /*
         *  외곽선
         */
        rings.add(
                toRingCoordinates(
                    polygon.getExteriorRing()
                )
        );

        /*
         *  내부 구멍
         */
        for(int i = 0; i < polygon.getNumInteriorRing(); i++){
            rings.add(
                    toRingCoordinates(
                            polygon.getInteriorRingN(i)
                    )
            );
        }

        return rings;
    }

    private List<List<Double>> toRingCoordinates(LineString ring){
        return Arrays.stream(ring.getCoordinates())
                .map(coordinate -> List.of(
                        coordinate.getX(),  // 경도
                        coordinate.getY()   // 위도
                ))
                .toList();
    }
}
