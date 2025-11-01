package com.example.boxwrapper.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * バリデーションエラー時にスローされる例外
 */
@Getter
public class ValidationException extends RuntimeException {

    private final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    private final Map<String, String> fieldErrors = new HashMap<>();

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * フィールドエラーを追加
     *
     * @param field フィールド名
     * @param errorMessage エラーメッセージ
     */
    public void addFieldError(String field, String errorMessage) {
        this.fieldErrors.put(field, errorMessage);
    }

    /**
     * 複数のフィールドエラーを追加
     *
     * @param errors エラーマップ
     */
    public void addFieldErrors(Map<String, String> errors) {
        this.fieldErrors.putAll(errors);
    }
}
