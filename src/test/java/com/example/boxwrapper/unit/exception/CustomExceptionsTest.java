package com.example.boxwrapper.unit.exception;

import com.example.boxwrapper.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Custom Exception Classes - Unit Tests")
class CustomExceptionsTest {

    @Test
    @DisplayName("BoxApiException - 基本的なコンストラクタ")
    void boxApiException_BasicConstructor() {
        // Given
        String message = "Box API呼び出しに失敗しました";

        // When
        BoxApiException exception = new BoxApiException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("BoxApiException - メッセージと原因を指定")
    void boxApiException_MessageAndCause() {
        // Given
        String message = "Box API呼び出しに失敗しました";
        Throwable cause = new RuntimeException("Network error");

        // When
        BoxApiException exception = new BoxApiException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("BoxApiException - HTTPステータスコードを指定")
    void boxApiException_WithHttpStatus() {
        // Given
        String message = "レート制限エラー";
        int statusCode = 429;

        // When
        BoxApiException exception = new BoxApiException(message, statusCode);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exception.getStatusCode()).isEqualTo(429);
    }

    @Test
    @DisplayName("BoxApiException - リトライ可能エラーの判定（5xx）")
    void boxApiException_IsRetryable_5xx() {
        // Given
        BoxApiException exception = new BoxApiException("Server error", 503);

        // When & Then
        assertThat(exception.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("BoxApiException - リトライ可能エラーの判定（429）")
    void boxApiException_IsRetryable_RateLimit() {
        // Given
        BoxApiException exception = new BoxApiException("Too many requests", 429);

        // When & Then
        assertThat(exception.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("BoxApiException - リトライ不可エラーの判定（4xx）")
    void boxApiException_IsNotRetryable_4xx() {
        // Given
        BoxApiException exception = new BoxApiException("Bad request", 400);

        // When & Then
        assertThat(exception.isRetryable()).isFalse();
    }

    @Test
    @DisplayName("AuthenticationException - 基本的なコンストラクタ")
    void authenticationException_BasicConstructor() {
        // Given
        String message = "認証に失敗しました";

        // When
        AuthenticationException exception = new AuthenticationException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("AuthenticationException - メッセージと原因を指定")
    void authenticationException_MessageAndCause() {
        // Given
        String message = "APIキーが無効です";
        Throwable cause = new RuntimeException("Invalid API key");

        // When
        AuthenticationException exception = new AuthenticationException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("ResourceNotFoundException - 基本的なコンストラクタ")
    void resourceNotFoundException_BasicConstructor() {
        // Given
        String resourceType = "File";
        String resourceId = "12345";

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(resourceType, resourceId);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).contains(resourceType);
        assertThat(exception.getMessage()).contains(resourceId);
        assertThat(exception.getResourceType()).isEqualTo(resourceType);
        assertThat(exception.getResourceId()).isEqualTo(resourceId);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("ResourceNotFoundException - カスタムメッセージ")
    void resourceNotFoundException_CustomMessage() {
        // Given
        String message = "指定されたフォルダが見つかりません";

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("ValidationException - 基本的なコンストラクタ")
    void validationException_BasicConstructor() {
        // Given
        String message = "バリデーションエラー";

        // When
        ValidationException exception = new ValidationException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("ValidationException - フィールドエラー付き")
    void validationException_WithFieldErrors() {
        // Given
        String message = "バリデーションエラーが発生しました";
        String field = "fileName";
        String errorMessage = "ファイル名は必須です";

        // When
        ValidationException exception = new ValidationException(message);
        exception.addFieldError(field, errorMessage);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getFieldErrors()).isNotEmpty();
        assertThat(exception.getFieldErrors()).containsKey(field);
        assertThat(exception.getFieldErrors().get(field)).isEqualTo(errorMessage);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("ValidationException - 複数のフィールドエラー")
    void validationException_MultipleFieldErrors() {
        // Given
        ValidationException exception = new ValidationException("複数のバリデーションエラー");

        // When
        exception.addFieldError("fileName", "ファイル名は必須です");
        exception.addFieldError("folderId", "フォルダIDが無効です");
        exception.addFieldError("fileSize", "ファイルサイズが大きすぎます");

        // Then
        assertThat(exception.getFieldErrors()).hasSize(3);
        assertThat(exception.getFieldErrors()).containsKeys("fileName", "folderId", "fileSize");
    }

    @Test
    @DisplayName("全てのカスタム例外がRuntimeExceptionを継承している")
    void allCustomExceptions_ExtendRuntimeException() {
        // When & Then
        assertThat(new BoxApiException("test")).isInstanceOf(RuntimeException.class);
        assertThat(new AuthenticationException("test")).isInstanceOf(RuntimeException.class);
        assertThat(new ResourceNotFoundException("test")).isInstanceOf(RuntimeException.class);
        assertThat(new ValidationException("test")).isInstanceOf(RuntimeException.class);
    }
}
