package com.zipdaproperty.domain.region.policy;

import org.springframework.stereotype.Component;

@Component
public class RegionBoundarySimplificationPolicy {

    private static final int ORIGINAL_LEVEL = 0;

    public int resolve(Integer kakaoMapLevel){

        /*
         *  미전달 시 원본 경계
         */
        if(kakaoMapLevel == null){
            return ORIGINAL_LEVEL;
        }

        /*
         *  kakao level이 작을수록 확대된 상세 지도
         */
        if(kakaoMapLevel <= 5){
            return 0;
        }

        if(kakaoMapLevel <= 8){
            return 1;
        }

        if(kakaoMapLevel <= 11){
            return 2;
        }

        /*
         *  Kakao level 12~14는 넓은 광역 지도
         */
        return 3;
    }
}
