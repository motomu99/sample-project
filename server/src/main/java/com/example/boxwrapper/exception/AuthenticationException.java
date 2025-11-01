package com.example.boxwrapper.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 認証失敗時にスローされる例外.
 *
 * <p>APIキーの検証失敗、Box認証情報の不正、トークン期限切れなど、
 * 認証関連のエラー時にスローされます。</p>
 *
 * <p>この例外が発生した場合、HTTPステータスコード401（Unauthorized）が
 * クライアントに返却されます。</p>
 *
 * @since 1.0.0
 */
@Getter
public class AuthenticationException extends RuntimeException {

    private final HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;

    /**
     * メッセージを指定して例外を生成します.
     *
     * @param message エラーメッセージ（例: "APIキーが無効です"）
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * メッセージと原因を指定して例外を生成します.
     *
     * @param message エラーメッセージ
     * @param cause 原因となった例外（例: Box SDKの認証エラー）
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
