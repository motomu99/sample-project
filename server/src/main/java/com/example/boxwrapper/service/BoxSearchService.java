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
 * Box 検索サービス.
 *
 * <p>Box SDKを使用した検索機能を提供します。
 * キーワード検索、ファイルタイプフィルタ、ページネーションに対応しています。</p>
 *
 * <p>検索結果はCaffeineキャッシュで5分間保持され、
 * 同じクエリに対する繰り返しアクセスのパフォーマンスを向上させます。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoxSearchService {

    private final BoxClientManager clientManager;
    private final RateLimiterManager rateLimiterManager;

    /**
     * ファイルおよびフォルダを検索します.
     *
     * <p>指定されたキーワードでBoxのコンテンツを検索し、
     * マッチしたアイテムの名前をリストで返します。</p>
     *
     * <p>検索オプション：
     * <ul>
     *   <li>type: "file"または"folder"でフィルタリング</li>
     *   <li>fileExtension: 拡張子でフィルタリング（例: "pdf"）</li>
     *   <li>offset/limit: ページネーション制御</li>
     * </ul>
     * </p>
     *
     * <p>検索結果はクエリとタイプの組み合わせでキャッシュされます。</p>
     *
     * @param apiKey 認証用のAPIキー
     * @param request 検索条件（クエリ、タイプ、ページネーション等）
     * @return 検索結果のアイテム名リスト
     * @throws BoxApiException Box API呼び出しに失敗した場合
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
            // Box SDK API変更により、offset/limitはsearchRangeメソッドで直接指定

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
