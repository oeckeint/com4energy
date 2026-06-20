package com.com4energy.processor.service;

import com.com4energy.persistence.filerecord.enums.FailureReason;
import com.com4energy.i18n.core.Messages;

import static com.com4energy.processor.common.IngestionCommonMessageKey.*;

public final class IngestionFailureReasonMessages {

    private IngestionFailureReasonMessages() {
    }

    public static String getDescription(FailureReason reason) {
        return switch (reason) {
            case FILE_NOT_FOUND -> Messages.get(FAILURE_REASON_FILE_NOT_FOUND);
            case INVALID_FILE_FORMAT -> Messages.get(FAILURE_REASON_INVALID_FILE_FORMAT);
            case INVALID_EXTENSION -> Messages.get(FAILURE_REASON_INVALID_EXTENSION);
            case INVALID_CONTENT_TYPE -> Messages.get(FAILURE_REASON_INVALID_CONTENT_TYPE);
            case FILE_TOO_LARGE -> Messages.get(FAILURE_REASON_FILE_TOO_LARGE);
            case FILE_IS_EMPTY -> Messages.get(FAILURE_REASON_FILE_IS_EMPTY);
            case INVALID_FILENAME -> Messages.get(FAILURE_REASON_INVALID_FILENAME);
            case PROCESSING_ERROR -> Messages.get(FAILURE_REASON_PROCESSING_ERROR);
            case DUPLICATED_FILE -> Messages.get(FAILURE_REASON_DUPLICATED_FILE);
            case DUPLICATED_FILENAME -> Messages.get(FAILURE_REASON_DUPLICATED_FILENAME);
            case DUPLICATED_ORIGINAL_FILENAME -> Messages.get(FAILURE_REASON_DUPLICATED_ORIGINAL_FILENAME);
            case DUPLICATED_CONTENT -> Messages.get(FAILURE_REASON_DUPLICATED_CONTENT);
            case DUPLICATED_VERSION -> Messages.get(FAILURE_REASON_DUPLICATED_VERSION);
            case UNAUTHORIZED_ACCESS -> Messages.get(FAILURE_REASON_UNAUTHORIZED_ACCESS);
            case TIMEOUT -> Messages.get(FAILURE_REASON_TIMEOUT);
            case MAX_RETRIES_EXCEEDED -> Messages.get(FAILURE_REASON_MAX_RETRIES_EXCEEDED);
            case NULL_FILE -> Messages.get(FAILURE_REASON_NULL_FILE);
            case STORAGE_ERROR -> Messages.get(FAILURE_REASON_STORAGE_ERROR);
            case UNKNOWN_ERROR -> Messages.get(FAILURE_REASON_UNKNOWN_ERROR);
            case STALE_LOCK -> Messages.get(FAILURE_REASON_STALE_LOCK);
            case SUPERSEDED_REVISION -> Messages.get(FAILURE_REASON_SUPERSEDED_REVISION);
            case CROSS_FAMILY_COLLISION -> Messages.get(FAILURE_REASON_CROSS_FAMILY_COLLISION);
        };
    }

}
