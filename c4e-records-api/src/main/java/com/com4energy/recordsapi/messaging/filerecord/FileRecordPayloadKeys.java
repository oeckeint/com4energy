package com.com4energy.recordsapi.messaging.filerecord;

/** Claves del payload para eventos de file record consumidos desde RabbitMQ. */
public final class FileRecordPayloadKeys {

    private FileRecordPayloadKeys() {
    }

    public static final String EVENT_TYPE = "eventType";
    public static final String SOURCE_ID = "sourceId";
    public static final String FILE_ID = "fileId";
    public static final String FILE_RECORD_ID = "fileRecordId";
    public static final String ID = "id";
    public static final String FILENAME = "filename";
    public static final String ORIGINAL_FILENAME = "originalFilename";
    public static final String EXTENSION = "extension";
    public static final String FILE_TYPE = "fileType";
    public static final String TYPE = "type";
    public static final String FINAL_PATH = "finalPath";
    public static final String STATUS = "status";
    public static final String ORIGIN = "origin";
    public static final String REASON = "reason";
    public static final String REASON_DESCRIPTION = "reasonDescription";
    public static final String LINE_NUMBER = "lineNumber";
    public static final String FAILED_LINE_NUMBER = "failedLineNumber";
    public static final String LINE = "line";
    public static final String LINE_REFERENCE = "lineReference";
    public static final String FAILED_LINE_REFERENCE = "failedLineReference";
    public static final String REFERENCE = "reference";
    public static final String COMMENT = "comment";
    public static final String CREATED_BY = "createdBy";
    public static final String USER = "user";
    public static final String USERNAME = "username";
    public static final String OCCURRED_AT = "occurredAt";
    public static final String METADATA = "metadata";

    public static final String UNKNOWN_FILE_RECORD_EVENT = "UNKNOWN_FILE_RECORD_EVENT";

}
