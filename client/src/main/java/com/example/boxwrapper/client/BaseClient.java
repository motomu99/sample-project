package com.example.boxwrapper.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;

/**
 * 全クライアントの基底クラス - 共通のHTTP操作を提供.
 *
 * @since 1.0.0
 */
@Slf4j
public abstract class BaseClient {

    protected final BoxWrapperClient mainClient;
    protected final ObjectMapper objectMapper;

    protected static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    protected BaseClient(BoxWrapperClient mainClient) {
        this.mainClient = mainClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * GETリクエストを実行します.
     *
     * @param endpoint エンドポイントパス
     * @param responseType レスポンスの型
     * @param <T> レスポンスの型
     * @return デシリアライズされたレスポンス
     * @throws BoxWrapperClientException API呼び出しに失敗した場合
     */
    protected <T> T get(String endpoint, Class<T> responseType) {
        String url = mainClient.getBaseUrl() + endpoint;

        Request request = new Request.Builder()
            .url(url)
            .addHeader("X-API-Key", mainClient.getApiKey())
            .get()
            .build();

        return execute(request, responseType);
    }

    /**
     * GETリクエストを実行します（TypeReferenceを使用）.
     *
     * @param endpoint エンドポイントパス
     * @param typeReference レスポンスの型参照
     * @param <T> レスポンスの型
     * @return デシリアライズされたレスポンス
     * @throws BoxWrapperClientException API呼び出しに失敗した場合
     */
    protected <T> T get(String endpoint, TypeReference<T> typeReference) {
        String url = mainClient.getBaseUrl() + endpoint;

        Request request = new Request.Builder()
            .url(url)
            .addHeader("X-API-Key", mainClient.getApiKey())
            .get()
            .build();

        return execute(request, typeReference);
    }

    /**
     * POSTリクエストを実行します.
     *
     * @param endpoint エンドポイントパス
     * @param requestBody リクエストボディ
     * @param responseType レスポンスの型
     * @param <T> レスポンスの型
     * @return デシリアライズされたレスポンス
     * @throws BoxWrapperClientException API呼び出しに失敗した場合
     */
    protected <T> T post(String endpoint, Object requestBody, Class<T> responseType) {
        String url = mainClient.getBaseUrl() + endpoint;

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            RequestBody body = RequestBody.create(jsonBody, JSON);

            Request request = new Request.Builder()
                .url(url)
                .addHeader("X-API-Key", mainClient.getApiKey())
                .post(body)
                .build();

            return execute(request, responseType);
        } catch (IOException e) {
            throw new BoxWrapperClientException("Failed to serialize request body", e);
        }
    }

    /**
     * DELETEリクエストを実行します.
     *
     * @param endpoint エンドポイントパス
     * @throws BoxWrapperClientException API呼び出しに失敗した場合
     */
    protected void delete(String endpoint) {
        String url = mainClient.getBaseUrl() + endpoint;

        Request request = new Request.Builder()
            .url(url)
            .addHeader("X-API-Key", mainClient.getApiKey())
            .delete()
            .build();

        try (Response response = mainClient.getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BoxWrapperClientException(
                    "Request failed with status: " + response.code() + " - " + response.message()
                );
            }
        } catch (IOException e) {
            throw new BoxWrapperClientException("Request execution failed", e);
        }
    }

    /**
     * リクエストを実行してレスポンスをデシリアライズします.
     *
     * @param request HTTPリクエスト
     * @param responseType レスポンスの型
     * @param <T> レスポンスの型
     * @return デシリアライズされたレスポンス
     * @throws BoxWrapperClientException API呼び出しに失敗した場合
     */
    private <T> T execute(Request request, Class<T> responseType) {
        try (Response response = mainClient.getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new BoxWrapperClientException(
                    "Request failed with status: " + response.code() + " - " + errorBody
                );
            }

            if (responseType == Void.class) {
                return null;
            }

            String responseBody = response.body() != null ? response.body().string() : "{}";
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException e) {
            throw new BoxWrapperClientException("Request execution failed", e);
        }
    }

    /**
     * リクエストを実行してレスポンスをデシリアライズします（TypeReferenceを使用）.
     *
     * @param request HTTPリクエスト
     * @param typeReference レスポンスの型参照
     * @param <T> レスポンスの型
     * @return デシリアライズされたレスポンス
     * @throws BoxWrapperClientException API呼び出しに失敗した場合
     */
    private <T> T execute(Request request, TypeReference<T> typeReference) {
        try (Response response = mainClient.getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new BoxWrapperClientException(
                    "Request failed with status: " + response.code() + " - " + errorBody
                );
            }

            String responseBody = response.body() != null ? response.body().string() : "{}";
            return objectMapper.readValue(responseBody, typeReference);
        } catch (IOException e) {
            throw new BoxWrapperClientException("Request execution failed", e);
        }
    }
}
