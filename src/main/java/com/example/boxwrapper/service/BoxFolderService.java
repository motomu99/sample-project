package com.example.boxwrapper.service;

import com.box.sdk.*;
import com.example.boxwrapper.client.BoxClientManager;
import com.example.boxwrapper.exception.BoxApiException;
import com.example.boxwrapper.exception.ResourceNotFoundException;
import com.example.boxwrapper.model.response.FolderInfoResponse;
import com.example.boxwrapper.utils.RateLimiterManager;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Box フォルダ操作サービス
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoxFolderService {

    private final BoxClientManager clientManager;
    private final RateLimiterManager rateLimiterManager;

    /**
     * フォルダを作成
     */
    @Retry(name = "boxApi")
    public FolderInfoResponse createFolder(String apiKey, String parentFolderId, String folderName) {
        try {
            if (!rateLimiterManager.tryConsume(apiKey)) {
                throw new BoxApiException("レート制限に達しました", 429);
            }

            BoxAPIConnection api = clientManager.getConnection(apiKey);
            BoxFolder parentFolder = new BoxFolder(api, parentFolderId);
            BoxFolder.Info folderInfo = parentFolder.createFolder(folderName);

            rateLimiterManager.handleSuccess(apiKey);
            log.info("Folder created successfully: {} (ID: {})", folderName, folderInfo.getID());

            return mapToFolderInfoResponse(folderInfo);

        } catch (BoxAPIException e) {
            if (e.getResponseCode() == 404) {
                throw new ResourceNotFoundException("Folder", parentFolderId);
            }
            if (e.getResponseCode() == 429) {
                rateLimiterManager.handleRateLimitExceeded(apiKey);
            }
            throw new BoxApiException("フォルダ作成に失敗しました: " + e.getMessage(),
                e.getResponseCode(), e);
        } catch (Exception e) {
            throw new BoxApiException("フォルダ作成に失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * フォルダ情報を取得
     */
    @Retry(name = "boxApi")
    @Cacheable(value = "folderInfo", key = "#folderId")
    public FolderInfoResponse getFolderInfo(String apiKey, String folderId) {
        try {
            if (!rateLimiterManager.tryConsume(apiKey)) {
                throw new BoxApiException("レート制限に達しました", 429);
            }

            BoxAPIConnection api = clientManager.getConnection(apiKey);
            BoxFolder folder = new BoxFolder(api, folderId);
            BoxFolder.Info info = folder.getInfo();

            rateLimiterManager.handleSuccess(apiKey);
            log.debug("Retrieved folder info: {}", folderId);

            return mapToFolderInfoResponse(info);

        } catch (BoxAPIException e) {
            if (e.getResponseCode() == 404) {
                throw new ResourceNotFoundException("Folder", folderId);
            }
            if (e.getResponseCode() == 429) {
                rateLimiterManager.handleRateLimitExceeded(apiKey);
            }
            throw new BoxApiException("フォルダ情報取得に失敗しました: " + e.getMessage(),
                e.getResponseCode(), e);
        } catch (Exception e) {
            throw new BoxApiException("フォルダ情報取得に失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * フォルダ内アイテム一覧を取得
     */
    @Retry(name = "boxApi")
    public List<String> listFolderItems(String apiKey, String folderId) {
        try {
            if (!rateLimiterManager.tryConsume(apiKey)) {
                throw new BoxApiException("レート制限に達しました", 429);
            }

            BoxAPIConnection api = clientManager.getConnection(apiKey);
            BoxFolder folder = new BoxFolder(api, folderId);

            List<String> items = new ArrayList<>();
            for (BoxItem.Info itemInfo : folder) {
                items.add(itemInfo.getName());
            }

            rateLimiterManager.handleSuccess(apiKey);
            log.debug("Retrieved {} items from folder: {}", items.size(), folderId);

            return items;

        } catch (BoxAPIException e) {
            if (e.getResponseCode() == 404) {
                throw new ResourceNotFoundException("Folder", folderId);
            }
            if (e.getResponseCode() == 429) {
                rateLimiterManager.handleRateLimitExceeded(apiKey);
            }
            throw new BoxApiException("フォルダアイテム取得に失敗しました: " + e.getMessage(),
                e.getResponseCode(), e);
        } catch (Exception e) {
            throw new BoxApiException("フォルダアイテム取得に失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * フォルダを削除
     */
    @Retry(name = "boxApi")
    @CacheEvict(value = "folderInfo", key = "#folderId")
    public void deleteFolder(String apiKey, String folderId, boolean recursive) {
        try {
            if (!rateLimiterManager.tryConsume(apiKey)) {
                throw new BoxApiException("レート制限に達しました", 429);
            }

            BoxAPIConnection api = clientManager.getConnection(apiKey);
            BoxFolder folder = new BoxFolder(api, folderId);
            folder.delete(recursive);

            rateLimiterManager.handleSuccess(apiKey);
            log.info("Folder deleted successfully: {}", folderId);

        } catch (BoxAPIException e) {
            if (e.getResponseCode() == 404) {
                throw new ResourceNotFoundException("Folder", folderId);
            }
            if (e.getResponseCode() == 429) {
                rateLimiterManager.handleRateLimitExceeded(apiKey);
            }
            throw new BoxApiException("フォルダ削除に失敗しました: " + e.getMessage(),
                e.getResponseCode(), e);
        } catch (Exception e) {
            throw new BoxApiException("フォルダ削除に失敗しました: " + e.getMessage(), e);
        }
    }

    /**
     * BoxFolder.InfoをFolderInfoResponseにマッピング
     */
    private FolderInfoResponse mapToFolderInfoResponse(BoxFolder.Info info) {
        return FolderInfoResponse.builder()
            .folderId(info.getID())
            .folderName(info.getName())
            .parentFolderId(info.getParent() != null ? info.getParent().getID() : null)
            .itemCount(info.getItemCollection() != null ?
                (int) info.getItemCollection().getTotalCount() : 0)
            .createdAt(toLocalDateTime(info.getCreatedAt()))
            .modifiedAt(toLocalDateTime(info.getModifiedAt()))
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
