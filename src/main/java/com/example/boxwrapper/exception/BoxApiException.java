package com.example.boxwrapper.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Box API呼び出し失敗時にスローされる例外.
 *
 * <p>Box SDKの操作中に発生したエラーをラップし、HTTPステータスコードと
 * リトライ可否の情報を提供します。</p>
 *
 * <p>リトライ可能なエラー：
 * <ul>
 *   <li>5xxエラー（サーバーエラー）</li>
 *   <li>429エラー（レート制限超過）</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 */
@Getter
public class BoxApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final int statusCode;

    /**
     * メッセージを指定して例外を生成します.
     *
     * <p>HTTPステータスコードは500（Internal Server Error）に設定されます。</p>
     *
     * @param message エラーメッセージ
     */
    public BoxApiException(String message) {
        super(message);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.statusCode = 500;
    }

    /**
     * メッセージと原因を指定して例外を生成します.
     *
     * <p>HTTPステータスコードは500（Internal Server Error）に設定されます。</p>
     *
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public BoxApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.statusCode = 500;
    }

    /**
     * メッセージとHTTPステータスコードを指定して例外を生成します.
     *
     * @param message エラーメッセージ
     * @param statusCode HTTPステータスコード（400-599）
     */
    public BoxApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.httpStatus = HttpStatus.valueOf(statusCode);
    }

    /**
     * メッセージ、HTTPステータスコード、原因を指定して例外を生成します.
     *
     * @param message エラーメッセージ
     * @param statusCode HTTPステータスコード（400-599）
     * @param cause 原因となった例外
     */
    public BoxApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.httpStatus = HttpStatus.valueOf(statusCode);
    }

    /**
     * このエラーがリトライ可能かどうかを判定します.
     *
     * <p>以下の場合にリトライ可能と判断されます：
     * <ul>
     *   <li>HTTPステータスコードが500以上（5xxエラー）</li>
     *   <li>HTTPステータスコードが429（Too Many Requests）</li>
     * </ul>
     * </p>
     *
     * @return リトライ可能な場合はtrue、それ以外はfalse
     */
    public boolean isRetryable() {
        return statusCode >= 500 || statusCode == 429;
    }
}
