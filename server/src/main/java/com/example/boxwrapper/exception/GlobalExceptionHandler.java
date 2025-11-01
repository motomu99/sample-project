package com.example.boxwrapper.exception;

import com.example.boxwrapper.model.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * グローバル例外ハンドラー
 * 全てのコントローラーで発生する例外を統一的に処理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * BoxApiException処理
     */
    @ExceptionHandler(BoxApiException.class)
    public ResponseEntity<ErrorResponse> handleBoxApiException(
            BoxApiException ex, WebRequest request) {

        log.error("Box API exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = buildErrorResponse(
            ex.getHttpStatus(),
            ex.getMessage(),
            request
        );

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * AuthenticationException処理
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        log.error("Authentication exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = buildErrorResponse(
            HttpStatus.UNAUTHORIZED,
            ex.getMessage(),
            request
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * ResourceNotFoundException処理
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        log.error("Resource not found exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = buildErrorResponse(
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            request
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * ValidationException処理
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, WebRequest request) {

        log.error("Validation exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            request
        );

        if (!ex.getFieldErrors().isEmpty()) {
            errorResponse.setFieldErrors(ex.getFieldErrors());
        }

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * IllegalArgumentException処理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        log.error("Illegal argument exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            request
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 汎用Exception処理（予期しないエラー）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage(),
            request
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * ErrorResponseオブジェクトを構築
     */
    private ErrorResponse buildErrorResponse(HttpStatus status, String message, WebRequest request) {
        String path = extractPath(request);
        String requestId = extractRequestId(request);

        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(path)
            .requestId(requestId)
            .build();
    }

    /**
     * リクエストパスを抽出
     */
    private String extractPath(WebRequest request) {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request.resolveReference("request");
            return httpRequest != null ? httpRequest.getRequestURI() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * リクエストIDを抽出
     */
    private String extractRequestId(WebRequest request) {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request.resolveReference("request");
            if (httpRequest != null) {
                Object requestId = httpRequest.getAttribute("requestId");
                return requestId != null ? requestId.toString() : null;
            }
        } catch (Exception e) {
            log.debug("Could not extract request ID", e);
        }
        return null;
    }
}
