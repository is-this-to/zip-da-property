package com.zipdaproperty.domain.property.audit.constant;

public final class PropertyAuditActionCode {

    public static final String PROPERTY_CREATED =
            "PROPERTY_CREATED";

    public static final String PROPERTY_UPDATED =
            "PROPERTY_UPDATED";

    public static final String PROPERTY_TRANSACTION_STATUS_CHANGED =
            "PROPERTY_TRANSACTION_STATUS_CHANGED";

    public static final String PROPERTY_SOFT_DELETED =
            "PROPERTY_SOFT_DELETED";

    public static final String PROPERTY_RESTORED =
            "PROPERTY_RESTORED";

    public static final String PROPERTY_VERIFICATION_REQUESTED =
            "PROPERTY_VERIFICATION_REQUESTED";

    public static final String PROPERTY_VERIFICATION_APPROVED =
            "PROPERTY_VERIFICATION_APPROVED";

    public static final String PROPERTY_VERIFICATION_REJECTED =
            "PROPERTY_VERIFICATION_REJECTED";

    private PropertyAuditActionCode() {
    }
}
