package com.zipdaproperty.domain.property.constant;

public enum PopularPropertyRegion {
    ALL(null),
    SEOUL("11"),
    BUSAN("26"),
    DAEGU("27"),
    INCHEON("28"),
    GWANGJU("29"),
    DAEJEON("30"),
    ULSAN("31"),
    SEJONG("36");

    private final String regionCodePrefix;

    PopularPropertyRegion(String regionCodePrefix) {
        this.regionCodePrefix = regionCodePrefix;
    }

    public String regionCodePrefix() {
        return regionCodePrefix;
    }
}
