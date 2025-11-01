package com.example.boxwrapper.client;

import com.example.boxwrapper.model.request.SearchRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;

/**
 * 検索クライアント.
 *
 * <p>Box SDK Wrapper REST APIの検索エンドポイントにアクセスします。</p>
 *
 * <p>使用例：
 * <pre>{@code
 * SearchClient searchClient = client.search();
 *
 * // シンプルな検索
 * List<String> results = searchClient.search("document");
 *
 * // 詳細検索
 * SearchRequest request = SearchRequest.builder()
 *     .query("annual report")
 *     .type("file")
 *     .fileExtension("pdf")
 *     .offset(0)
 *     .limit(10)
 *     .build();
 * List<String> results = searchClient.search(request);
 * }</pre>
 * </p>
 *
 * @since 1.0.0
 */
@Slf4j
public class SearchClient extends BaseClient {

    public SearchClient(BoxWrapperClient mainClient) {
        super(mainClient);
    }

    /**
     * シンプルな検索を実行します.
     *
     * @param query 検索クエリ
     * @return 検索結果のアイテム名リスト
     * @throws BoxWrapperClientException 検索に失敗した場合
     */
    public List<String> search(String query) {
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .build();
        return search(request);
    }

    /**
     * 詳細な検索を実行します.
     *
     * @param searchRequest 検索リクエスト
     * @return 検索結果のアイテム名リスト
     * @throws BoxWrapperClientException 検索に失敗した場合
     */
    public List<String> search(SearchRequest searchRequest) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(mainClient.getBaseUrl() + "/api/v1/search").newBuilder();
        urlBuilder.addQueryParameter("query", searchRequest.getQuery());

        if (searchRequest.getType() != null) {
            urlBuilder.addQueryParameter("type", searchRequest.getType());
        }
        if (searchRequest.getFileExtension() != null) {
            urlBuilder.addQueryParameter("fileExtension", searchRequest.getFileExtension());
        }
        if (searchRequest.getOffset() != null) {
            urlBuilder.addQueryParameter("offset", String.valueOf(searchRequest.getOffset()));
        }
        if (searchRequest.getLimit() != null) {
            urlBuilder.addQueryParameter("limit", String.valueOf(searchRequest.getLimit()));
        }

        Request request = new Request.Builder()
            .url(urlBuilder.build())
            .addHeader("X-API-Key", mainClient.getApiKey())
            .get()
            .build();

        try (Response response = mainClient.getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BoxWrapperClientException(
                    "Search failed with status: " + response.code()
                );
            }

            String responseBody = response.body() != null ? response.body().string() : "[]";
            return objectMapper.readValue(responseBody, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            throw new BoxWrapperClientException("Search failed", e);
        }
    }
}
