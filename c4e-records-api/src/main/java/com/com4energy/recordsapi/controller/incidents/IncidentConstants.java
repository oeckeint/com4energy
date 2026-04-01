package com.com4energy.recordsapi.controller.incidents;

import com.com4energy.i18n.core.Messages;
import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;
import com.com4energy.recordsapi.controller.common.ApiConstants;

final class IncidentConstants {

    static final String BASE_PATH          = ApiConstants.API_V1 + "/incidents";
    static final String OPEN_PATH          = "/open";
    static final String RECENT_PATH        = "/recent";
    static final String BY_SERVICE_PATH    = "/service/{serviceName}";
    static final String BY_SEVERITY_PATH   = "/severity/{severity}";
    static final String UPDATE_STATUS_PATH = "/{id}/status";
    static final String ADD_COMMENT_PATH   = "/{id}/comments";
    static final String RESOLVE_PATH       = "/{id}/resolve";

    private IncidentConstants() {
        throw new IllegalStateException(Messages.get(RecordsApiCommonMessageKey.UTILITY_CLASS));
    }
}
