package com.com4energy.recordsapi.exception.handler;

import com.com4energy.event.publisher.exception.PublisherException;
import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;
import com.com4energy.i18n.core.Messages;
import com.com4energy.recordsapi.controller.common.ApiConstants;
import com.com4energy.recordsapi.exception.BusinessException;
import com.com4energy.recordsapi.exception.ResourceNotFoundException;
import com.com4energy.recordsapi.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        // Sin este handler, el catch-all de Exception convertía un 4xx (p. ej. parámetros
        // inválidos en cell-origins) en 500. Respeta el status que trae la excepción.
        int status = ex.getStatusCode().value();
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();

        ApiError error = new ApiError(status, message);

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {

        ApiError error = new ApiError(
                ApiConstants.HTTP_NOT_FOUND,
                ex.getMessage()
        );

        return ResponseEntity.status(ApiConstants.HTTP_NOT_FOUND).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {

        ApiError error = new ApiError(
                ApiConstants.HTTP_CONFLICT,
                ex.getMessage()
        );

        return ResponseEntity.status(ApiConstants.HTTP_CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {

        ApiError error = new ApiError(
                ApiConstants.HTTP_BAD_REQUEST,
                ex.getMessage()
        );

        return ResponseEntity.status(ApiConstants.HTTP_BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage(HttpMessageNotReadableException ex) {

        Throwable root = getRootCause(ex);
        String message = root instanceof IllegalArgumentException && root.getMessage() != null
                ? root.getMessage()
                : Messages.get(RecordsApiCommonMessageKey.ERROR_UNEXPECTED_NO_PARAM);

        ApiError error = new ApiError(
                ApiConstants.HTTP_BAD_REQUEST,
                message
        );

        return ResponseEntity.status(ApiConstants.HTTP_BAD_REQUEST).body(error);
    }

    @ExceptionHandler(PublisherException.class)
    public ResponseEntity<ApiError> handlePublisher(PublisherException ex) {

        ApiError error = new ApiError(
                ApiConstants.HTTP_INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );

        return ResponseEntity.status(ApiConstants.HTTP_INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex, HttpServletRequest request) {
        if (isSseRequest(request)) {
            return ResponseEntity.status(ApiConstants.HTTP_INTERNAL_SERVER_ERROR).build();
        }

        ApiError error = new ApiError(
                ApiConstants.HTTP_INTERNAL_SERVER_ERROR,
                Messages.get(RecordsApiCommonMessageKey.ERROR_UNEXPECTED_NO_PARAM)
        );

        return ResponseEntity.status(ApiConstants.HTTP_INTERNAL_SERVER_ERROR).body(error);
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private boolean isSseRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

}
