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
 * Box ファイル操作サービス
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoxFileService {

    private final BoxClientManager clientManager;
    private final RateLimiterManager rateLimiterManager;

    /**
     * ファイルをアップロード
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
     * ファイル情報を取得
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
     * ファイルをダウンロード
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
     * ファイルを削除
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
