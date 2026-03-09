package com.com4energy.recordsapi.controller.common;

import com.com4energy.recordsapi.common.MessageKey;
import com.com4energy.recordsapi.common.Messages;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Helper class for building consistent HTTP responses across all API endpoints.
 * Handles empty collections with informative headers.
 */
public final class ResponseHelper {

    private ResponseHelper() {
        throw new IllegalStateException(Messages.get(MessageKey.UTILITY_CLASS));
    }

    /**
     * Wraps a Page result with an informative header when no data is found.
     *
     * @param page the page result (may be empty)
     * @return ResponseEntity with 200 OK status, or 200 with info header if empty
     */
    public static <T> ResponseEntity<Page<T>> ok(Page<T> page) {
        if (page.isEmpty()) {
            return ResponseEntity.ok()
                .header(Messages.get(MessageKey.HEADER_INFO), Messages.get(MessageKey.NO_DATA_FOUND_CRITERIA))
                .body(page);
        }
        return ResponseEntity.ok(page);
    }

    /**
     * Wraps a List result with an informative header when no data is found.
     *
     * @param list the list result (may be empty)
     * @return ResponseEntity with 200 OK status, or 200 with info header if empty
     */
    public static <T> ResponseEntity<List<T>> ok(List<T> list) {
        if (list.isEmpty()) {
            return ResponseEntity.ok()
                .header(Messages.get(MessageKey.HEADER_INFO), Messages.get(MessageKey.NO_DATA_FOUND_CRITERIA))
                .body(list);
        }
        return ResponseEntity.ok(list);
    }

    /**
     * Wraps any Collection result with an informative header when no data is found.
     * Use this for other Collection implementations (Set, Queue, etc.)
     *
     * @param collection the collection result (may be empty)
     * @return ResponseEntity with 200 OK status, or 200 with info header if empty
     */
    public static <T, C extends Collection<T>> ResponseEntity<C> ok(C collection) {
        if (collection.isEmpty()) {
            return ResponseEntity.ok()
                .header(Messages.get(MessageKey.HEADER_INFO), Messages.get(MessageKey.NO_DATA_FOUND_CRITERIA))
                .body(collection);
        }
        return ResponseEntity.ok(collection);
    }

    /**
     * Wraps a single object result. Returns 200 OK with the object,
     * or 404 Not Found if Optional is empty.
     *
     * @param optional the optional result
     * @return ResponseEntity with 200 OK or 404 Not Found
     */
    public static <T> ResponseEntity<T> ok(Optional<T> optional) {
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(optional.get());
    }

    /**
     * Wraps a single object result directly.
     *
     * @param object the object to wrap (may be null)
     * @return ResponseEntity with 200 OK, or 404 Not Found if null
     */
    public static <T> ResponseEntity<T> ok(T object) {
        if (object == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(object);
    }

    /**
     * Custom response with specific message and status.
     * Useful for more granular control over responses.
     *
     * @param data the data to return
     * @param messageKey the message key to display in header
     * @return ResponseEntity with the data and custom header
     */
    public static <T> ResponseEntity<T> okWithMessage(T data, MessageKey messageKey) {
        return ResponseEntity.ok()
            .header(Messages.get(MessageKey.HEADER_INFO), Messages.get(messageKey))
            .body(data);
    }

    /**
     * Returns a collection with informative header, regardless of whether it's empty.
     * Useful when you want to always show a message.
     *
     * @param collection the collection result
     * @param messageKey the message key to display
     * @return ResponseEntity with custom message header
     */
    public static <T, C extends Collection<T>> ResponseEntity<C> okWithAlwaysMessage(C collection, MessageKey messageKey) {
        return ResponseEntity.ok()
            .header(Messages.get(MessageKey.HEADER_INFO), Messages.get(messageKey))
            .body(collection);
    }
}

