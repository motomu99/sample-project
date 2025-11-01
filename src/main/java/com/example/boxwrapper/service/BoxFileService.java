package com.example.boxwrapper.service;

import com.box.sdk.*;
import com.example.boxwrapper.client.BoxClientManager;
import com.example.boxwrapper.exception.BoxApiException;
import com.example.boxwrapper.exception.ResourceNotFoundException;
import com.example.boxwrapper.model.response.FileInfoResponse;
import com.example.boxwrapper.model.response.FileUploadResponse;
import com.example.boxwrapper.utils.RateLimiterManager;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Box ファイル操作サービス.
 *
 * <p>Box SDKを使用したファイル操作（アップロード、ダウンロード、情報取得、削除）を
 * 提供します。レート制限、リトライ、キャッシング機能を統合しています。</p>
 *
 * <p>全てのメソッドはResilience4jによる自動リトライを適用し、
 * 一時的なエラーから自動回復します。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoxFileService {

    private final BoxClientManager clientManager;
    private final RateLimiterManager rateLimiterManager;

    /**
     * ファイルをBoxにアップロードします.
     *
     * <p>指定されたフォルダにマルチパートファイルをアップロードし、
     * アップロード結果の情報（ファイルID、名前、サイズなど）を返します。</p>
     *
     * <p>レート制限を自動的に適用し、制限超過時は429エラーをスローします。
     * 一時的なエラー（5xx、429）の場合は自動的にリトライされます。</p>
     *
     * @param apiKey 認証用のAPIキー
     * @param folderId アップロード先のフォルダID（例: "0"はルートフォルダ）
     * @param file アップロードするマルチパートファイル
     * @return アップロードされたファイルの情報
     * @throws BoxApiException Box API呼び出しに失敗した場合
     * @throws ResourceNotFoundException フォルダが存在しない場合（404）
     */
    @Retry(name = "boxApi")
    public FileUploadResponse uploadFile(String apiKey, String folderId, MultipartFile file) {
        try {
            if (!rateLimiterManager.tryConsume(apiKey)) {
                throw new BoxApiException("レート制限に達しました。しばらく待ってから再試行してください。", 429);
            }

            BoxAPIConnection api = clientManager.getConnection(apiKey);
            BoxFolder folder = new BoxFolder(api, folderId);

            try (InputStream stream = file.getInputStream()) {
                BoxFile.Info fileInfo = folder.uploadFile(stream, file.getOriginalFilename());

                rateLimiterManager.handleSuccess(apiKey);
                log.info("File uploaded successfully: {} (ID: {})",
                    fileInfo.getName(), fileInfo.getID());

                return mapToUploadResponse(fileInfo);
            }

        } catch (BoxAPIException e) {
            if (e.getResponseCode() == 429) {
                rateLimiterManager.handleRateLimitExceeded(apiKey);
            }
            throw new BoxApiException("ファイルアップロードに失敗しました: " + e.getMessage(),
                e.getResponseCode(), e);
        } catch (Exception e) {
            throw new BoxApiException("ファイルアップロードに失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * ファイルのメタデータ情報を取得します.
     *
     * <p>ファイルID、名前、サイズ、親フォルダID、作成日時、更新日時、SHA1ハッシュなどの
     * 詳細情報を取得します。結果は5分間キャッシュされます。</p>
     *
     * @param apiKey 認証用のAPIキー
     * @param fileId 情報を取得するファイルのID
     * @return ファイルのメタデータ情報
     * @throws BoxApiException Box API呼び出しに失敗した場合
     * @throws ResourceNotFoundException ファイルが存在しない場合（404）
     */
    @Retry(name = "boxApi")
    @Cacheable(value = "fileMetadata", key = "#fileId")
    public FileInfoResponse getFileInfo(String apiKey, String fileId) {
        try {
            if (!rateLimiterManager.tryConsume(apiKey)) {
                throw new BoxApiException("レート制限に達しました", 429);
            }

            BoxAPIConnection api = clientManager.getConnection(apiKey);
            BoxFile file = new BoxFile(api, fileId);
            BoxFile.Info info = file.getInfo();

            rateLimiterManager.handleSuccess(apiKey);
            log.debug("Retrieved file info: {}", fileId);

            return mapToFileInfoResponse(info);

        } catch (BoxAPIException e) {
            if (e.getResponseCode() == 404) {
                throw new ResourceNotFoundException("File", fileId);
            }
            if (e.getResponseCode() == 429) {
                rateLimiterManager.handleRateLimitExceeded(apiKey);
            }
            throw new BoxApiException("ファイル情報取得に失敗しました: " + e.getMessage(),
                e.getResponseCode(), e);
        } catch (Exception e) {
            throw new BoxApiException("ファイル情報取得に失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * ファイルの内容をダウンロードします.
     *
     * <p>ファイル全体をバイト配列として取得します。
     * 大容量ファイルの場合は、メモリ使用量に注意してください。</p>
     *
     * @param apiKey 認証用のAPIキー
     * @param fileId ダウンロードするファイルのID
     * @return ファイルの内容（バイト配列）
     * @throws BoxApiException Box API呼び出しに失敗した場合
     * @throws ResourceNotFoundException ファイルが存在しない場合（404）
     */
    @Retry(name = "boxApi")
    public byte[] downloadFile(String apiKey, String fileId) {
        try {
            if (!rateLimiterManager.tryConsume(apiKey)) {
                throw new BoxApiException("レート制限に達しました", 429);
            }

            BoxAPIConnection api = clientManager.getConnection(apiKey);
            BoxFile file = new BoxFile(api, fileId);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            file.download(outputStream);

            rateLimiterManager.handleSuccess(apiKey);
            log.info("File downloaded successfully: {}", fileId);

            return outputStream.toByteArray();

        } catch (BoxAPIException e) {
            if (e.getResponseCode() == 404) {
                throw new ResourceNotFoundException("File", fileId);
            }
            if (e.getResponseCode() == 429) {
                rateLimiterManager.handleRateLimitExceeded(apiKey);
            }
            throw new BoxApiException("ファイルダウンロードに失敗しました: " + e.getMessage(),
                e.getResponseCode(), e);
        } catch (Exception e) {
            throw new BoxApiException("ファイルダウンロードに失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * ファイルを削除します.
     *
     * <p>指定されたファイルをBoxから完全に削除します。
     * 削除されたファイルはゴミ箱に移動され、一定期間後に完全削除されます。</p>
     *
     * <p>削除成功時、キャッシュされていたファイルメタデータも自動的にクリアされます。</p>
     *
     * @param apiKey 認証用のAPIキー
     * @param fileId 削除するファイルのID
     * @throws BoxApiException Box API呼び出しに失敗した場合
     * @throws ResourceNotFoundException ファイルが存在しない場合（404）
     */
    @Retry(name = "boxApi")
    @CacheEvict(value = "fileMetadata", key = "#fileId")
    public void deleteFile(String apiKey, String fileId) {
        try {
            if (!rateLimiterManager.tryConsume(apiKey)) {
                throw new BoxApiException("レート制限に達しました", 429);
            }

            BoxAPIConnection api = clientManager.getConnection(apiKey);
            BoxFile file = new BoxFile(api, fileId);
            file.delete();

            rateLimiterManager.handleSuccess(apiKey);
            log.info("File deleted successfully: {}", fileId);

        } catch (BoxAPIException e) {
            if (e.getResponseCode() == 404) {
                throw new ResourceNotFoundException("File", fileId);
            }
            if (e.getResponseCode() == 429) {
                rateLimiterManager.handleRateLimitExceeded(apiKey);
            }
            throw new BoxApiException("ファイル削除に失敗しました: " + e.getMessage(),
                e.getResponseCode(), e);
        } catch (Exception e) {
            throw new BoxApiException("ファイル削除に失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * BoxFile.InfoをFileUploadResponseにマッピング
     */
    private FileUploadResponse mapToUploadResponse(BoxFile.Info info) {
        return FileUploadResponse.builder()
            .fileId(info.getID())
            .fileName(info.getName())
            .size(info.getSize())
            .createdAt(toLocalDateTime(info.getCreatedAt()))
            .build();
    }

    /**
     * BoxFile.InfoをFileInfoResponseにマッピング
     */
    private FileInfoResponse mapToFileInfoResponse(BoxFile.Info info) {
        return FileInfoResponse.builder()
            .fileId(info.getID())
            .fileName(info.getName())
            .size(info.getSize())
            .parentFolderId(info.getParent() != null ? info.getParent().getID() : null)
            .createdAt(toLocalDateTime(info.getCreatedAt()))
            .modifiedAt(toLocalDateTime(info.getModifiedAt()))
            .sha1(info.getSha1())
            .build();
    }

    /**
     * DateをLocalDateTimeに変換
     */
    private LocalDateTime toLocalDateTime(java.util.Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
    }
}
