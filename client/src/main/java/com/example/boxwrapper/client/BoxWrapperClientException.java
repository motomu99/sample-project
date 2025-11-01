package com.example.boxwrapper.client;

/**
 * Box Wrapper クライアントで発生する例外.
 *
 * <p>API呼び出しの失敗、ネットワークエラー、デシリアライズエラーなど、
 * クライアント側で発生する全ての例外を表します。</p>
 *
 * @since 1.0.0
 */
public class BoxWrapperClientException extends RuntimeException {

    /**
     * メッセージを指定して例外を作成します.
     *
     * @param message エラーメッセージ
     */
    public BoxWrapperClientException(String message) {
        super(message);
    }

    /**
     * メッセージと原因を指定して例外を作成します.
     *
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public BoxWrapperClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
