package com.com4energy.recordsapi.controller.filerecords;

import com.com4energy.i18n.core.Messages;
import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;
import com.com4energy.recordsapi.controller.common.ApiConstants;

public final class FileRecordConstants {

    public static final String BASE_PATH = ApiConstants.API_V1 + "/file-records";

    private FileRecordConstants() {
        throw new IllegalStateException(Messages.get(RecordsApiCommonMessageKey.UTILITY_CLASS));
    }
}

