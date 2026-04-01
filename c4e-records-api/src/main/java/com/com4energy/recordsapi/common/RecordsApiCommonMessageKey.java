package com.com4energy.recordsapi.common;

import com.com4energy.i18n.core.MessageKey;

public enum RecordsApiCommonMessageKey implements MessageKey {

    // ---------------------------
    // GENERAL / SYSTEM MESSAGES
    // ---------------------------
    SYSTEM_ERROR("system.error"),
    ERROR_UNEXPECTED_NO_PARAM("error.unexpected.no_param"),
    ERROR_UNEXPECTED_USE_PARAM("error.unexpected.w_param"),
    UTILITY_CLASS("utility.class.message"),
    HEADER_INFO("header.info.message"),

    // ---------------------------
    // ENDPOINTS
    // ---------------------------
    ENDPOINT_WORKS("endpoint.works"),

    // ---------------------------
    // USER MESSAGES
    // ---------------------------
    INVALID_EMAIL("validation.email.invalid"),
    USER_NOT_FOUND("user.not.found"),
    USER_CREATED_SUCCESSFULLY("user.created.successfully"),

    // ---------------------------
    // INCIDENT MESSAGES
    // ---------------------------
    INCIDENT_EVENT_ID_EMPTY("incident.event.id.empty"),
    INCIDENT_EVENT_SERVICE_NAME_EMPTY("incident.event.service.name.empty"),
    INCIDENT_EVENT_EXCEPTION_TYPE_EMPTY("incident.event.exception.type.empty"),
    INCIDENT_EVENT_SEVERITY_NULL("incident.event.severity.null"),
    INCIDENT_EVENT_CATEGORY_NULL("incident.event.category.null"),
    INCIDENT_EVENT_PAYLOAD_TOO_LARGE("incident.event.payload.too.large"),
    INCIDENT_SAVED("incident.saved"),
    INCIDENT_LOG_NOT_FOUND("incident.log.not.found"),
    INCIDENT_PAYLOAD_JSON_PARSE_ERROR("incident.payload.json.parse.error"),

    // ---------------------------
    // MEDIDAQH / BUSINESS DOMAIN MESSAGES
    // ---------------------------
    MEDIDA_NOT_FOUND("medidaqh.not.found"),
    NO_DATA_FOUND_CRITERIA("no.data.found.criteria");

    private final String key;

    RecordsApiCommonMessageKey(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}