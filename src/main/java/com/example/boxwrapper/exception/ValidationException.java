package com.example.boxwrapper.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * バリデーションエラー時にスローされる例外.
 *
 * <p>リクエストパラメータ、ボディ、ファイルなどのバリデーションに失敗した場合に
 * スローされます。フィールド単位のエラー情報を保持できます。</p>
 *
 * <p>この例外が発生した場合、HTTPステータスコード400（Bad Request）が
 * クライアントに返却され、フィールドエラーの詳細情報も含まれます。</p>
 *
 * @since 1.0.0
 */
@Getter
public class ValidationException extends RuntimeException {

    private final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    private final Map<String, String> fieldErrors = new HashMap<>();

    /**
     * メッセージを指定して例外を生成します.
     *
     * @param message エラーメッセージ
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * メッセージと原因を指定して例外を生成します.
     *
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * フィールドエラーを追加します.
     *
     * <p>既に同じフィールド名のエラーが存在する場合は上書きされます。</p>
     *
     * @param field フィールド名（例: "fileName", "folderId"）
     * @param errorMessage フィールドに対するエラーメッセージ
     */
    public void addFieldError(String field, String errorMessage) {
        this.fieldErrors.put(field, errorMessage);
    }

    /**
     * 複数のフィールドエラーを一括で追加します.
     *
     * <p>既存のフィールドエラーは保持され、新しいエラーが追加されます。
     * 同じフィールド名が存在する場合は上書きされます。</p>
     *
     * @param errors フィールド名をキー、エラーメッセージを値とするマップ
     */
    public void addFieldErrors(Map<String, String> errors) {
        this.fieldErrors.putAll(errors);
    }
}
