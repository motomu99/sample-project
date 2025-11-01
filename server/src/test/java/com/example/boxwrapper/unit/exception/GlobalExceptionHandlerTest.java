package com.example.boxwrapper.unit.exception;

import com.example.boxwrapper.exception.*;
import com.example.boxwrapper.model.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler - Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    private MockHttpServletRequest httpServletRequest;

    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRequestURI("/api/v1/files/12345");
        httpServletRequest.setAttribute("requestId", "test-request-id-123");
        webRequest = new ServletWebRequest(httpServletRequest);
    }

    @Test
    @DisplayName("BoxApiException処理 - 正常系")
    void handleBoxApiException_Success() {
        // Given
        BoxApiException exception = new BoxApiException("Box API呼び出しに失敗しました", 500);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBoxApiException(
            exception, webRequest
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Box API呼び出しに失敗しました");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/files/12345");
        assertThat(response.getBody().getRequestId()).isEqualTo("test-request-id-123");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("BoxApiException処理 - レート制限エラー")
    void handleBoxApiException_RateLimit() {
        // Given
        BoxApiException exception = new BoxApiException("レート制限を超えました", 429);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBoxApiException(
            exception, webRequest
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().getStatus()).isEqualTo(429);
        assertThat(response.getBody().getError()).isEqualTo("Too Many Requests");
    }

    @Test
    @DisplayName("AuthenticationException処理 - 正常系")
    void handleAuthenticationException_Success() {
        // Given
        AuthenticationException exception = new AuthenticationException("APIキーが無効です");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(
            exception, webRequest
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        assertThat(response.getBody().getMessage()).isEqualTo("APIキーが無効です");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/files/12345");
        assertThat(response.getBody().getRequestId()).isEqualTo("test-request-id-123");
    }

    @Test
    @DisplayName("ResourceNotFoundException処理 - 正常系")
    void handleResourceNotFoundException_Success() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("File", "12345");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(
            exception, webRequest
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).contains("File");
        assertThat(response.getBody().getMessage()).contains("12345");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/files/12345");
    }

    @Test
    @DisplayName("ValidationException処理 - フィールドエラーなし")
    void handleValidationException_NoFieldErrors() {
        // Given
        ValidationException exception = new ValidationException("バリデーションエラーが発生しました");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(
            exception, webRequest
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("バリデーションエラーが発生しました");
        assertThat(response.getBody().getFieldErrors()).isNullOrEmpty();
    }

    @Test
    @DisplayName("ValidationException処理 - フィールドエラーあり")
    void handleValidationException_WithFieldErrors() {
        // Given
        ValidationException exception = new ValidationException("バリデーションエラーが発生しました");
        exception.addFieldError("fileName", "ファイル名は必須です");
        exception.addFieldError("folderId", "フォルダIDが無効です");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(
            exception, webRequest
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFieldErrors()).isNotEmpty();
        assertThat(response.getBody().getFieldErrors()).hasSize(2);
        assertThat(response.getBody().getFieldErrors()).containsKeys("fileName", "folderId");
        assertThat(response.getBody().getFieldErrors().get("fileName")).isEqualTo("ファイル名は必須です");
    }

    @Test
    @DisplayName("IllegalArgumentException処理 - 正常系")
    void handleIllegalArgumentException_Success() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("不正な引数です");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(
            exception, webRequest
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("不正な引数です");
    }

    @Test
    @DisplayName("汎用Exception処理 - 正常系")
    void handleGenericException_Success() {
        // Given
        Exception exception = new Exception("予期しないエラーが発生しました");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(
            exception, webRequest
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("予期しないエラーが発生しました");
    }

    @Test
    @DisplayName("RequestIdがnullの場合の処理")
    void handleException_RequestIdNull() {
        // Given
        httpServletRequest.removeAttribute("requestId");
        BoxApiException exception = new BoxApiException("エラー");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBoxApiException(
            exception, webRequest
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRequestId()).isNull();
    }
}
