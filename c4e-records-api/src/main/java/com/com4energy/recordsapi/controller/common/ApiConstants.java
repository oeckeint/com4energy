package com.com4energy.recordsapi.controller.common;

import com.com4energy.recordsapi.common.MessageKey;
import com.com4energy.recordsapi.common.Messages;

public final class ApiConstants {

    public static final String TEST_ALL_PATH = "/testall";
    public static final String ID_PATH = "/{id}";

    public static final int DEFAULT_PAGE_SIZE = 24;

    private ApiConstants() {
        throw new IllegalStateException(Messages.get(MessageKey.UTILITY_CLASS));
    }

}
