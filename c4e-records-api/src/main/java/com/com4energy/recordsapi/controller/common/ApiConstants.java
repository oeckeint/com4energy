package com.com4energy.recordsapi.controller.common;

import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;
import com.com4energy.i18n.core.Messages;

public final class ApiConstants {

    // API Paths
    public static final String API_V1      = "/api/v1";
    public static final String TEST_ALL_PATH = "/testall";
    public static final String ID_PATH     = "/{id}";

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 24;

    // HTTP Status codes
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_INTERNAL_SERVER_ERROR = 500;

    private ApiConstants() {
        throw new IllegalStateException(Messages.get(RecordsApiCommonMessageKey.UTILITY_CLASS));
    }

}
