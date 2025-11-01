package com.example.boxwrapper.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Box API呼び出し失敗時にスローされる例外
 */
@Getter
public class BoxApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final int statusCode;

    public BoxApiException(String message) {
        super(message);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.statusCode = 500;
    }

    public BoxApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.statusCode = 500;
    }

    public BoxApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.httpStatus = HttpStatus.valueOf(statusCode);
    }

    public BoxApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.httpStatus = HttpStatus.valueOf(statusCode);
    }

    /**
     * リトライ可能なエラーかどうかを判定
     *
     * @return 5xxエラーまたは429エラーの場合はtrue
     */
    public boolean isRetryable() {
        return statusCode >= 500 || statusCode == 429;
    }
}
