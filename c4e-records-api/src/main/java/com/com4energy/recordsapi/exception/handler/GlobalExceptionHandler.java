package com.com4energy.recordsapi.exception.handler;

import com.com4energy.recordsapi.common.MessageKey;
import com.com4energy.recordsapi.common.Messages;
import com.com4energy.recordsapi.controller.common.ApiConstants;
import com.com4energy.recordsapi.exception.BusinessException;
import com.com4energy.recordsapi.exception.ResourceNotFoundException;
import com.com4energy.recordsapi.response.ApiError;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {

        ApiError error = new ApiError(
                ApiConstants.HTTP_INTERNAL_SERVER_ERROR,
                Messages.get(MessageKey.ERROR_UNEXPECTED_NO_PARAM)
        );

        return ResponseEntity.status(ApiConstants.HTTP_INTERNAL_SERVER_ERROR).body(error);
    }

}
