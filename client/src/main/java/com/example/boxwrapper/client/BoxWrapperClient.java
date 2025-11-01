package com.example.boxwrapper.client;

import lombok.Getter;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * メインクライアントクラス - Box SDK Wrapper REST APIへのアクセスを提供.
 *
 * <p>このクライアントは、Box SDK Wrapper REST APIの全てのエンドポイントに
 * アクセスするための統一されたインターフェースを提供します。</p>
 *
 * <p>使用例：
 * <pre>{@code
 * BoxWrapperClient client = BoxWrapperClient.builder()
 *     .baseUrl("http://localhost:8080")
 *     .apiKey("your-api-key")
 *     .build();
 *
 * // ファイル操作
 * FileUploadResponse response = client.files().uploadFile(folderId, file);
 *
 * // フォルダ操作
 * FolderInfoResponse folder = client.folders().createFolder(parentId, folderName);
 *
 * // 検索
 * List<String> results = client.search().search("query", SearchRequest.builder()...);
 * }</pre>
 * </p>
 *
 * @since 1.0.0
 */
@Getter
public class BoxWrapperClient {

    private final String baseUrl;
    private final String apiKey;
    private final OkHttpClient httpClient;

    private final FileClient files;
    private final FolderClient folders;
    private final SearchClient search;
    private final JobClient jobs;

    private BoxWrapperClient(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.apiKey = builder.apiKey;
        this.httpClient = builder.httpClient != null ? builder.httpClient : createDefaultHttpClient();

        // Initialize endpoint clients
        this.files = new FileClient(this);
        this.folders = new FolderClient(this);
        this.search = new SearchClient(this);
        this.jobs = new JobClient(this);
    }

    /**
     * デフォルトのHTTPクライアントを作成.
     */
    private static OkHttpClient createDefaultHttpClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    }

    /**
     * BoxWrapperClientのビルダーを返します.
     *
     * @return ビルダーインスタンス
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * BoxWrapperClientのビルダークラス.
     */
    public static class Builder {
        private String baseUrl;
        private String apiKey;
        private OkHttpClient httpClient;

        /**
         * APIのベースURLを設定します.
         *
         * @param baseUrl ベースURL（例: "http://localhost:8080"）
         * @return ビルダーインスタンス
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * 認証用のAPIキーを設定します.
         *
         * @param apiKey APIキー
         * @return ビルダーインスタンス
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * カスタムのHTTPクライアントを設定します.
         *
         * <p>設定しない場合、デフォルトのOkHttpClientが使用されます。</p>
         *
         * @param httpClient カスタムHTTPクライアント
         * @return ビルダーインスタンス
         */
        public Builder httpClient(OkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * BoxWrapperClientを構築します.
         *
         * @return BoxWrapperClientインスタンス
         * @throws IllegalStateException baseUrlまたはapiKeyが設定されていない場合
         */
        public BoxWrapperClient build() {
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalStateException("baseUrl must be set");
            }
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalStateException("apiKey must be set");
            }
            return new BoxWrapperClient(this);
        }
    }
}
