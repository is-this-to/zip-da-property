package com.zipdaproperty.domain.property.event.constant;

public final class PropertyEventType {

    public static final String PROPERTY_CREATED = "PROPERTY_CREATED";
    public static final String PROPERTY_UPDATED = "PROPERTY_UPDATED";
    public static final String PROPERTY_PUBLISHED = "PROPERTY_PUBLISHED";
    public static final String PROPERTY_HIDDEN = "PROPERTY_HIDDEN";
    public static final String PROPERTY_REJECTED = "PROPERTY_REJECTED";
    public static final String PROPERTY_RESUBMITTED = "PROPERTY_RESUBMITTED";
    public static final String PROPERTY_REACTIVATED = "PROPERTY_REACTIVATED";
    public static final String PROPERTY_COMPLETED = "PROPERTY_COMPLETED";
    public static final String PROPERTY_DELETED = "PROPERTY_DELETED";
    public static final String PROPERTY_VERIFICATION_REQUESTED =
            "PROPERTY_VERIFICATION_REQUESTED";
    public static final String PROPERTY_VERIFICATION_APPROVED =
            "PROPERTY_VERIFICATION_APPROVED";
    public static final String PROPERTY_VERIFICATION_REJECTED =
            "PROPERTY_VERIFICATION_REJECTED";
    public static final String PROPERTY_VERIFICATION_EXPIRED =
            "PROPERTY_VERIFICATION_EXPIRED";

    private PropertyEventType() {
    }
}
