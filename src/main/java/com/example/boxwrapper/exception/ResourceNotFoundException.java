package com.example.boxwrapper.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * リソース未検出時にスローされる例外.
 *
 * <p>指定されたファイル、フォルダ、ジョブなどのリソースが存在しない場合に
 * スローされます。</p>
 *
 * <p>この例外が発生した場合、HTTPステータスコード404（Not Found）が
 * クライアントに返却されます。</p>
 *
 * @since 1.0.0
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final HttpStatus httpStatus = HttpStatus.NOT_FOUND;
    private String resourceType;
    private String resourceId;

    /**
     * カスタムメッセージを指定して例外を生成します.
     *
     * @param message エラーメッセージ
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * リソースタイプとIDを指定して例外を生成します.
     *
     * <p>メッセージは自動的に「{resourceType} (ID: {resourceId}) が見つかりません」
     * の形式で生成されます。</p>
     *
     * @param resourceType リソースの種類（例: "File", "Folder", "Job"）
     * @param resourceId リソースのID
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s (ID: %s) が見つかりません", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * メッセージと原因を指定して例外を生成します.
     *
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
