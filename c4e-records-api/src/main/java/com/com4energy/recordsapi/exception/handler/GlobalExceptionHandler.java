package com.com4energy.recordsapi.exception.handler;

import com.com4energy.recordsapi.common.MessageKey;
import com.com4energy.recordsapi.common.Messages;
import com.com4energy.recordsapi.controller.common.ApiConstants;
import com.com4energy.recordsapi.exception.BusinessException;
import com.com4energy.recordsapi.exception.ResourceNotFoundException;
import com.com4energy.recordsapi.response.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

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
                : Messages.get(MessageKey.ERROR_UNEXPECTED_NO_PARAM);

        ApiError error = new ApiError(
                ApiConstants.HTTP_BAD_REQUEST,
                message
        );

        return ResponseEntity.status(ApiConstants.HTTP_BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {

        ApiError error = new ApiError(
                ApiConstants.HTTP_INTERNAL_SERVER_ERROR,
                Messages.get(MessageKey.ERROR_UNEXPECTED_NO_PARAM)
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

}
