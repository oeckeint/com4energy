package com.com4energy.recordsapi.common;

public enum MessageKey {

    ERROR_UNEXPECTED_NO_PARAM("error.unexpected.no_param"),
    ERROR_UNEXPECTED_USE_PARAM("error.unexpected.w_param"),

    ENDPOINT_WORKS("endpoint.works"),

    HEADER_INFO("header.info.message"),

    INVALID_EMAIL("validation.email.invalid"),

    MEDIDA_NOT_FOUND("medidaqh.not.found"),

    NO_DATA_FOUND_CRITERIA("no.data.found.criteria"),

    SYSTEM_ERROR("system.error"),

    USER_CREATED_SUCCESSFULLY("user.created.successfully"),
    USER_NOT_FOUND("user.not.found"),

    UTILITY_CLASS("utility.class.message")
    ;

    private final String key;

    MessageKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

}
