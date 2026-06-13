package com.com4energy.recordsapi.controller.filerecords;

import com.com4energy.i18n.core.Messages;
import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;
import com.com4energy.recordsapi.controller.common.ApiConstants;

public final class FileRecordMetricsConstants {

    public static final String BASE_PATH = ApiConstants.API_V1 + "/file-records/metrics";
    public static final String SUMMARY_PATH = "/summary";
    public static final String BY_TYPE_PATH = "/by-type";

    private FileRecordMetricsConstants() {
        throw new IllegalStateException(Messages.get(RecordsApiCommonMessageKey.UTILITY_CLASS));
    }
}

