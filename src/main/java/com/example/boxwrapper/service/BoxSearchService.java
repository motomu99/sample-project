package com.example.boxwrapper.service;

import com.box.sdk.*;
import com.example.boxwrapper.client.BoxClientManager;
import com.example.boxwrapper.exception.BoxApiException;
import com.example.boxwrapper.model.request.SearchRequest;
import com.example.boxwrapper.utils.RateLimiterManager;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Box 検索サービス
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoxSearchService {

    private final BoxClientManager clientManager;
    private final RateLimiterManager rateLimiterManager;

    /**
     * ファイル/フォルダを検索
     */
    @Retry(name = "boxApi")
    @Cacheable(value = "searchResults", key = "#request.query + '_' + #request.type")
    public List<String> search(String apiKey, SearchRequest request) {
        try {
            if (!rateLimiterManager.tryConsume(apiKey)) {
                throw new BoxApiException("レート制限に達しました", 429);
            }

            BoxAPIConnection api = clientManager.getConnection(apiKey);

            BoxSearch boxSearch = new BoxSearch(api);
            BoxSearchParameters searchParams = new BoxSearchParameters();
            searchParams.setQuery(request.getQuery());

            // Type filter
            if (request.getType() != null && !request.getType().isEmpty()) {
                if ("file".equalsIgnoreCase(request.getType())) {
                    searchParams.setType("file");
                } else if ("folder".equalsIgnoreCase(request.getType())) {
                    searchParams.setType("folder");
                }
            }

            // File extension filter
            if (request.getFileExtension() != null && !request.getFileExtension().isEmpty()) {
                List<String> extensions = List.of(request.getFileExtension());
                searchParams.setFileExtensions(extensions);
            }

            // Pagination
            if (request.getOffset() != null) {
                searchParams.setOffset(request.getOffset());
            }
            if (request.getLimit() != null) {
                searchParams.setLimit(request.getLimit());
            }

            PartialCollection<BoxItem.Info> searchResults = boxSearch.searchRange(
                request.getOffset() != null ? request.getOffset() : 0,
                request.getLimit() != null ? request.getLimit() : 100,
                searchParams
            );

            List<String> results = new ArrayList<>();
            for (BoxItem.Info itemInfo : searchResults) {
                results.add(itemInfo.getName());
            }

            rateLimiterManager.handleSuccess(apiKey);
            log.info("Search completed: {} results for query '{}'", results.size(), request.getQuery());

            return results;

        } catch (BoxAPIException e) {
            if (e.getResponseCode() == 429) {
                rateLimiterManager.handleRateLimitExceeded(apiKey);
            }
            throw new BoxApiException("検索に失敗しました: " + e.getMessage(),
                e.getResponseCode(), e);
        } catch (Exception e) {
            throw new BoxApiException("検索に失敗しました: " + e.getMessage(), e);
        }
    }
}
