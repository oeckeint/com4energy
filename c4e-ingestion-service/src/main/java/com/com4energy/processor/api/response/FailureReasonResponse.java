package com.com4energy.processor.api.response;

import com.com4energy.persistence.filerecord.enums.FailureReason;
import com.com4energy.processor.service.IngestionFailureReasonMessages;

public record FailureReasonResponse(
        String code,
        String message) {

    public static FailureReasonResponse from(FailureReason reason) {
        return new FailureReasonResponse(reason.name(), IngestionFailureReasonMessages.getDescription(reason));
    }

}
